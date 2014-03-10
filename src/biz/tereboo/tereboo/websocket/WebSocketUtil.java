package biz.tereboo.tereboo.websocket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import biz.tereboo.tereboo.util.AquesTalk2Util;
import biz.tereboo.tereboo.util.SpeechRecognizerUtil;
import biz.tereboo.tereboo.util.SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener;
import android.content.Context;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class WebSocketUtil {
	private static String TAG = WebSocketUtil.class.getName();

	private Context context;
	private WebSocketClient client;
	private WebSocketUtilEventsListener listener;

	private static WebSocketUtil instance;

	public static WebSocketUtil getInstance(Context context,String url,WebSocketUtilEventsListener listener) {
		if (instance == null) {
			synchronized(SpeechRecognizerUtil.class) {
				instance = new WebSocketUtil(context, url,listener);
		    }
		}
	    return instance;
	}

	private WebSocketUtil(Context context,String url,WebSocketUtilEventsListener listener){
		this.context = context;
		this.listener = listener;
		try{
			URI uri = new URI(url);
			this.client = new WebSocketClient(uri){
				@Override
				public void onOpen(ServerHandshake handshake) {
					WebSocketUtil.this.listener.onOpen(handshake);
				}

				@Override
				public void onMessage(String message) {
					WebSocketUtil.this.listener.onMessage(message);
				}

				@Override
				public void onError(Exception e) {
					WebSocketUtil.this.listener.onError(e);
				}


				@Override
				public void onClose(int code, String reason, boolean remote) {
					WebSocketUtil.this.listener.onClose(code, reason, remote);
				}
			};
		}
		catch(Exception e){

		}
	}

	/** 接続
	 *
	 */
	public void connect(){
		this.client.connect();
	}

	/** 切断
	 *
	 */
	public void close(){
		this.client.close();
	}

	/** バイナリ データ送信
	 *
	 * @param data
	 */
	public void send(byte[] data){
		this.client.send(data);
	}

	/** テキスト データ送信
	 *
	 */
	public void send(String txt){
		this.client.send(txt);
	}

	/** Socketの状態
	 *
	 * @return
	 */
	public boolean isClosed(){
		return this.client.getConnection().isClosed();
	}
	/** イベントリスナー
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static abstract class WebSocketUtilEventsListener {
		///接続時
		public abstract void onOpen(ServerHandshake handshake);
		//メッセージが届いた
		public abstract void onMessage(String message);
		//エラー
		public abstract void onError(Exception e);
		//切断時
		public abstract void onClose(int code, String reason, boolean remote);
	}
}
