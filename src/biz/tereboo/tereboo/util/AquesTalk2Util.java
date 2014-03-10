package biz.tereboo.tereboo.util;

import java.io.InputStream;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;
import aquestalk2.AquesTalk2;

public class AquesTalk2Util {
	private static final String TAG = "AquesTalk2Util";
	private static Context context;

	private static AquesTalk2Util instance;
	private static AquesTalk2 aquesTalk2;
	private static AudioTrack audioTrack = null;

	private static Handler handler = new Handler();
	//再生完了時のコールバック実行
	private static Runnable completeCallback;

	private AquesTalk2Util(Context context){
		this.context = context;
		this.aquesTalk2 = new AquesTalk2();
	}

	public static AquesTalk2Util getInstance(Context context){
		if (instance == null) {
			synchronized(AquesTalk2Util.class) {
				instance = new AquesTalk2Util(context);
		    }
		}
		return instance;
	}

	/** phontのデータを取得する
	 *
	 * @param resID
	 * @return
	 */
	public static byte[] loadPhontDat(int resID){
		byte[] phontDat = null;
		if(resID == 0) return null;
		try{
			InputStream	in = context.getResources().openRawResource(resID);
			int size = in.available();
			phontDat = new byte[size];
	    	in.read(phontDat);
		}catch(Exception e){

		}
		return phontDat;
	}

	/**　音声合成
	 *
	 * @param txt
	 * @param resID
	 * @param speed
	 */
	public static void speech(String txt,int resID,int speed){
		speech(txt,resID,speed,null);
	}

	/**　音声合成
	 *
	 * @param txt
	 * @param resID
	 * @param speed
	 * @param callback
	 */
	public static void speech(String txt,int resID,int speed,Runnable callback){
		completeCallback = callback;

		//音声合成 して Wav データを取得
		byte[] phontDat = loadPhontDat(resID);
		final byte[] wav = aquesTalk2.syntheWav(txt, speed, phontDat);
		//Log.i(TAG, "wav:"+wav);
		if(wav != null){
			//音声再生
			playAudioTrack(wav);
		}
	}

	/** 音声再生
	 *
	 * @param wav
	 */
	private static void playAudioTrack(byte[] wav){
		if(audioTrack != null){
			//ステータス確認
			Log.i(TAG,"audioTrack:"+audioTrack.getState());
			if(AudioTrack.PLAYSTATE_PLAYING == audioTrack.getState()){
				//再生中なら無視
				return;
			}
		}

		audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                8000,//サンプリング周波数
                AudioFormat.CHANNEL_CONFIGURATION_MONO,//モノラル
                AudioFormat.ENCODING_PCM_16BIT,//16bitPCM
                8000*2*40,    // バッファサイズ、ここでは最大１０秒としている
                AudioTrack.MODE_STATIC);

		Log.d(TAG, "completeCallback:"+completeCallback);
		if(completeCallback != null){
			audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
						public void onPeriodicNotification(AudioTrack track) {
						}

						// 再生完了時のコールバック
						public void onMarkerReached(AudioTrack track) {
							Log.d(TAG, "track:"+track);
							if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
								// 再生完了したので発声停止
								track.stop();
								handler.post(completeCallback);
							}
						}
					});
		}

		try{
			//音声再生
			int headerSize = 44;
			int size = wav.length-headerSize;
			audioTrack.write(wav, headerSize, size);
			audioTrack.setNotificationMarkerPosition(size);
			audioTrack.play();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
