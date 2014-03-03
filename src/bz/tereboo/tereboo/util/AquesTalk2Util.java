package bz.tereboo.tereboo.util;

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
	private static Handler handler = new Handler();
	private static Context context;

	private static AquesTalk2Util instance;
	private static AquesTalk2 aquesTalk2;

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
	 * @param resID
	 * @param txt
	 */
	public static void speech(String txt,int resID,int speed){
		//音声合成 して Wav データを取得
		byte[] phontDat = loadPhontDat(resID);
		final byte[] wav = aquesTalk2.syntheWav(txt, speed, phontDat);
		Log.i(TAG, "wav:"+wav);
		if(wav != null){
			//音声再生
			handler.post(new Runnable() {
				@Override
				public void run() {
					playAudioTrack(wav);
				}
			});
		}
	}

	/** 音声再生
	 *
	 * @param wav
	 */
	private static void playAudioTrack(byte[] wav){

		AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                8000,//サンプリング周波数
                AudioFormat.CHANNEL_CONFIGURATION_MONO,//モノラル
                AudioFormat.ENCODING_PCM_16BIT,//16bitPCM
                8000*2*10,    // バッファサイズ、ここでは最大１０秒としている
                AudioTrack.MODE_STATIC);

		//音声再生
		audioTrack.write(wav, 44, wav.length-44);
		audioTrack.setNotificationMarkerPosition(wav.length);
		audioTrack.play();
	}
}
