package bz.tereboo.tereboo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

/** 音声認識関連ラッパークラス
 *
 * @author furukawanobuyuki
 *
 */
public class SpeechRecognizerUtil implements RecognitionListener{
	private static Context context;
	private static SpeechRecognizerUtilEventsListener listener;
	private static SpeechRecognizerUtil instance = null;

	private static SpeechRecognizer speechRecognizer;

	/** コンストラクタ
	 *
	 */
	private SpeechRecognizerUtil(Context context,SpeechRecognizerUtilEventsListener listener){
		this.context = context;
		this.listener = listener;
	}

	/** インスタンス作成
	 *
	 * @param context
	 * @return
	 */
	public static SpeechRecognizerUtil getInstance(Context context,SpeechRecognizerUtilEventsListener listener) {
		if (instance == null) {
			synchronized(SpeechRecognizerUtil.class) {
				instance = new SpeechRecognizerUtil(context,listener);
		    	speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
		    	speechRecognizer.setRecognitionListener(instance);
		    }
		}
	    return instance;
	}

	/** 音声認識開始
	 *
	 */
	public static void start(){
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toString());

        speechRecognizer.startListening(intent);
	}

	// RecognitionListener -------------
	@Override
	public void onBeginningOfSpeech() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndOfSpeech() {
		//Toast.makeText(context.getApplicationContext(), "入力終了", Toast.LENGTH_SHORT).show();
		//入力終了
		this.listener.onEndOfSpeech();
	}

	@Override
	public void onError(int error) {
		this.listener.onError(error);
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		//Toast.makeText(context.getApplicationContext(), "認識開始", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onResults(Bundle results) {
		//音声認識 成功
		String recognizedList = "";
        ArrayList<String> candidates = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        //リスナに通知
        this.listener.onResults(candidates);
	}

	@Override
	public void onRmsChanged(float rmsdB) {
		// TODO Auto-generated method stub

	}

	/** 音声認識リスナ
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static abstract class SpeechRecognizerUtilEventsListener {
	    /**
         * 音声認識の準備が完了した時
         */
        public abstract void onEndOfSpeech();

        /** エラー発生時
         *
         * @param error
         */
        public abstract void onError(int error);

        /**
         * 認識結果の文字列リストを処理
         */
        public abstract void onResults(List<String> results);

	}
}
