package biz.tereboo.tereboo;

import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import biz.tereboo.tereboo.bluetooth.BluetoothUtil;
import biz.tereboo.tereboo.bluetooth.DeviceListActivity;
import biz.tereboo.tereboo.http.HttpPostTask;
import biz.tereboo.tereboo.util.AquesTalk2Util;
import biz.tereboo.tereboo.util.SpeechRecognizerUtil;
import biz.tereboo.tereboo.util.TerebooApiUtil;
import biz.tereboo.tereboo.util.TerebooCmdParser;
import biz.tereboo.tereboo.websocket.WebSocketUtil;
import bz.tereboo.tereboo.R;

public class MainActivity extends Activity{
	private static final String TAG = "MainActivity";

	//UI更新用のハンドラー
	private Handler mainHandler = new Handler();

	//音声認識 ラッパークラス
	private SpeechRecognizerUtil speechRecognizerUtil = null;

	//音声合成 ラッパークラス
	private AquesTalk2Util aquesTalk2Util = null;

	//Bluetooth ラッパークラス
	private BluetoothUtil bluetoothUtil = null;

	//WebSocket ラッパークラス
	private WebSocketUtil webSocketUtil = null;
	private static final String webSocketServerURL = "ws://153.121.52.22:8000/";

	private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    //しりとりモード
    private boolean shiritoriMode = false;
    private Runnable shiritoriCallback = null;
	private CookieStore shiritoriCookieStore = null;

	//雑談モード
    private boolean zatudanMode = false;
    private Runnable zatudanCallback = null;
	private CookieStore zatudanCookieStore = null;

	//会話のリソースID
	private static final int speechResID = R.raw.aq_yukkuri;
	private static final int speechSpeed = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechRecognizerUtil = SpeechRecognizerUtil.getInstance(this,this.speechReclistener);
        aquesTalk2Util = AquesTalk2Util.getInstance(this);

        //Bluetoothの状態の確認
        bluetoothUtil = BluetoothUtil.getInstance(this);
        if(false == bluetoothUtil.isSupport()){
        	//Bluetooth 未対応端末
        	//TODO アラーを表示してアプリ終了
        	Log.i(TAG,"Bluetooth 未対応端末でした");
        	Toast.makeText(getApplicationContext(), "Bluetooth 未対応端末のようです。", Toast.LENGTH_SHORT).show();
        	this.finish();
        	return;
        }

        if(false == bluetoothUtil.isEnabled()){
        	//BluetoothがONになっていないのでONにするように促す
        	bluetoothUtil.showEnableBluetoothDialog(this, REQUEST_ENABLE_BLUETOOTH);
        }

        //WebSocket
        this.webSocketUtil = new WebSocketUtil(getApplicationContext(),webSocketServerURL,this.webSocketEventsListener);
        this.webSocketUtil.connect();

        ((Button) findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//音声認 識開始
				speechRecognizerUtil.start();
			}
		});

        ((Button) findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Bluetooth デバイス一覧を表示 & 接続
				showDeviceList();
			}
		});

        ((Button) findViewById(R.id.button3)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Bluetooth でデータ送信
				byte[] send = "c fujitv\n".getBytes();
				bluetoothUtil.writeChatService(send);
			}
		});

        ((Button) findViewById(R.id.button4)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Bluetooth でデータ送信
				byte[] send = "p on\n".getBytes();
				bluetoothUtil.writeChatService(send);
			}
		});

        ((Button) findViewById(R.id.button5)).setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				//Bluetooth でデータ送信
 				byte[] send = "v up\n".getBytes();
 				bluetoothUtil.writeChatService(send);
 			}
 		});
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Bluetooth 接続を終了
        this.bluetoothUtil.closeChatService();
        //WebSocket 切断
        this.webSocketUtil.close();
    }

    /** Bluetooth デバイス一覧
     *
     */
    private void showDeviceList(){
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener speechReclistener = new  SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener(){

		@Override
		public void onEndOfSpeech() {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "認識 終了", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onError(int error) {
			Toast.makeText(getApplicationContext(), "認識 エラー", Toast.LENGTH_SHORT).show();
			Runnable callback = null;
			/*
			if( (shiritoriMode == true) ||(zatudanMode == true) ){
				callback = new Runnable() {
					@Override
					public void run() {
						try{
							Thread.sleep(500);
						}catch(Exception e){

						}
						//再度 音声認識モードになる
						speechRecognizerUtil.start();
					}
				};
			}*/
			aquesTalk2Util.speech("ききとれませんでした。", speechResID, speechSpeed, callback);
		}

		@Override
		public void onResults(List<String> results) {
			//音声認識 終了
			//コマンド解析
			Map<String,String> result = TerebooCmdParser.parse(results);
			String cmd = result.get(TerebooCmdParser.RESULT_CMD);
			String q = result.get(TerebooCmdParser.RESULT_TXT);

			if(shiritoriMode == true){
				//しりとりモード中
				if("shiritori_stop".equals(cmd)){
					//しりとりモード終了
					// "テレブー しりとりはおわり" で終了
					shiritoriMode = false;
					shiritoriCookieStore.clear();
					TerebooApiUtil.shiritori(getApplicationContext(), siritoriHandler, shiritoriCookieStore, q, false);
				}else{
					//しりとり中 なのでワードだけ送信
					q = results.get(0);
					TerebooApiUtil.shiritori(getApplicationContext(), siritoriHandler, shiritoriCookieStore, q, true);
				}
				return;
			}
			//雑談モード
			if(zatudanMode == true){
				//雑談モード中
				if("zatudan_stop".equals(cmd)){
					//雑談モード終了
					// "テレブー 雑談はおわり" で終了
					zatudanMode = false;
					zatudanCookieStore.clear();
					TerebooApiUtil.shiritori(getApplicationContext(), zatudanHandler, zatudanCookieStore, q, false);
				}else{
					//雑談中 なのでワードだけ送信
					q = results.get(0);
					TerebooApiUtil.shiritori(getApplicationContext(), zatudanHandler, zatudanCookieStore, q, true);
				}
				return;
			}

			if(cmd != null){
				Toast.makeText(getApplicationContext(), "認識 コマンド:"+cmd, Toast.LENGTH_SHORT).show();
				//TOTD APIサーバーへ通知してレスポンスを貰う
				//TODO APIサーバからのレスポンスを音声合成
				if( "tadaima".equals(cmd) ){
					//Toast.makeText(getApplicationContext(), "speech", Toast.LENGTH_SHORT).show();
					aquesTalk2Util.speech("おかえり、てれびつけるね。", speechResID, speechSpeed);

					//BluetoothでテレビON
					bluetoothUtil.writeChatService("p on\n".getBytes());
				}
				else if( "oyasumi".equals(cmd) ){
					//Toast.makeText(getApplicationContext(), "speech", Toast.LENGTH_SHORT).show();
					aquesTalk2Util.speech("おやすみなさい。", speechResID, speechSpeed);

					//Bluetooth で テレビ OFF
					bluetoothUtil.writeChatService("p off\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TBS.equals(cmd) ){
					aquesTalk2Util.speech("てぃーびーえすにするね。", speechResID, speechSpeed);

					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tbs\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TVTOKYO.equals(cmd) ){
					aquesTalk2Util.speech("てれとうにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tvtokyo\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_FUJITV.equals(cmd) ){
					aquesTalk2Util.speech("ふぃじてれびにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c fujitv\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TV_ASAHI.equals(cmd) ){
					aquesTalk2Util.speech("てれあさにするね。", speechResID, speechSpeed);

					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tv-asahi\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_NTV.equals(cmd) ){
					aquesTalk2Util.speech("にってれにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c ntv\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_NHK.equals(cmd) ){
					aquesTalk2Util.speech("えぬえちけーにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c nhk\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_E_TELE.equals(cmd) ){
					aquesTalk2Util.speech("いーてれにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c e-tele\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_MXTV.equals(cmd) ){
					aquesTalk2Util.speech("えむえっくすにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c mxtv\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TELETAMA.equals(cmd) ){
					aquesTalk2Util.speech("てれたまにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c teletama\n".getBytes());
				}
				else if( "shiritori_start".equals(cmd) ){
					//しりとり モードに入る
					shiritoriMode = true;
					q = "しりとりをやろうよ";
					TerebooApiUtil.shiritori(getApplicationContext(), siritoriHandler, shiritoriCookieStore, q, true);
				}
				else if( "zatudan_start".equals(cmd) ){
					//雑談 モードに入る
					zatudanMode = true;
					q = "ざつだんをしようよ";
					TerebooApiUtil.shiritori(getApplicationContext(), zatudanHandler, zatudanCookieStore, q, true);
				}
				else if( "buy".equals(cmd) ){
					//これ買って
					// サーバにPOSTして その時間の 現在チャンネルの番組の商品を探して 購入させる？
				}
			}
			else{
				Toast.makeText(getApplicationContext(), "認識 コマンドを認識できませんでした", Toast.LENGTH_SHORT).show();
				Runnable callback = null;
				/*
				callback = new Runnable() {
					@Override
					public void run() {
						//再度 音声認識モードになる
						speechRecognizerUtil.start();
					}
				};
				*/
				aquesTalk2Util.speech("ききとれなかったぶー", speechResID, speechSpeed, callback);
			}
		}
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult requestCode:"+requestCode+ " resultCode:"+ resultCode);
         if (resultCode == RESULT_OK) {
        	 switch (requestCode) {
             case REQUEST_ENABLE_BLUETOOTH:
            	 //BluetoothをONにした時
                 Log.d(TAG, "ENABLE_BLUETOOTH SUCCESS");
            	 break;
             case REQUEST_CONNECT_DEVICE_SECURE:
                 // デバイスと接続
                 if (resultCode == Activity.RESULT_OK) {
                	 bluetoothUtil.connectChatService(data, true);
                 }
                 break;
             case REQUEST_ENABLE_BT:
            	 break;
        	 }
         }
    }

    /** HTTP POSTリクエスト
     *
     */
    private HttpPostTask.HttpPostHandler postHandler = new HttpPostTask.HttpPostHandler(){
		@Override
		public void onPostSuccess(int statusCode, String response,CookieStore cookieStore) {
			//送信レスポンス
			Log.d(TAG, "Success statusCode:"+statusCode);
			Log.d(TAG, response);

			Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			// 失敗時レスポンス
			Log.d(TAG, "Failed statusCode:"+statusCode);
			Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}
    };


	/** しりとり結果を受け取る
	 *
	 */
	HttpPostTask.HttpPostHandler siritoriHandler = new HttpPostTask.HttpPostHandler(){

		@Override
		public void onPostSuccess(int statusCode, String response,CookieStore cookieStore) {
			//しりとりの返事をJSONで受け取る
			try{
				//継続の為クッキーを保存
				shiritoriCookieStore = cookieStore;

				JSONObject rootObj = new JSONObject(response);
				//String utt = rootObj.getString("utt");
				String yomi = rootObj.getString("yomi");

				Toast.makeText(getApplicationContext(), "yomi:"+yomi, Toast.LENGTH_SHORT).show();
				Log.d(TAG, "shiritoriMode:"+shiritoriMode+" yomi:"+yomi);

				//yomiの値を音声合成で出力する
				this.speech(yomi);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}

		private void speech(String yomi){
			//yomiの値を音声合成で出力する
			if(shiritoriMode == true){
				//しりとりモードの場合だけ、連続音声認識
				shiritoriCallback = new Runnable() {
					@Override
					public void run() {
						if(shiritoriMode == true){
							//連続音声認識
							Log.d(TAG, "speech callback");
							speechRecognizerUtil.start();
						}
					}
				};
			}

			aquesTalk2Util.speech(yomi, speechResID, speechSpeed, shiritoriCallback);
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}
	};

	/** 雑談を受け取る
	 *
	 */
	HttpPostTask.HttpPostHandler zatudanHandler = new HttpPostTask.HttpPostHandler(){
		@Override
		public void onPostSuccess(int statusCode, String response,CookieStore cookieStore) {
			//雑談の返事をJSONで受け取る
			try{
				//継続の為クッキーを保存
				zatudanCookieStore = cookieStore;

				JSONObject rootObj = new JSONObject(response);
				//String utt = rootObj.getString("utt");
				String yomi = rootObj.getString("yomi");

				Toast.makeText(getApplicationContext(), "yomi:"+yomi, Toast.LENGTH_SHORT).show();
				Log.d(TAG, "zatudanMode:"+zatudanMode+" yomi:"+yomi);

				speech(yomi);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}

		//yomiの値を音声合成で出力する
		private void speech(String yomi){
			if(zatudanMode == true){
				//しりとりモードの場合だけ、連続音声認識
				zatudanCallback = new Runnable() {
					@Override
					public void run() {
						if(zatudanMode == true){
							//連続音声認識
							Log.d(TAG, "speech callback");
							speechRecognizerUtil.start();
						}
					}
				};
			}

			aquesTalk2Util.speech(yomi, speechResID, speechSpeed, zatudanCallback);
		}
		@Override
		public void onPostFailed(int statusCode, String response) {
			Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}
	};

	/** WebSocketのイベントリスナー
	 *
	 */
	public WebSocketUtil.WebSocketUtilEventsListener webSocketEventsListener = new WebSocketUtil.WebSocketUtilEventsListener(){
		@Override
		public void onOpen(ServerHandshake handshake) {
			Log.d(TAG, "ws Open");
		}

		@Override
		public void onMessage(final String message) {
			Log.d(TAG, "ws message:"+message);
			try {
				JSONObject rootObj = new JSONObject(message);
				//TODO 仮フォーマットをパースして画面&音声合成
				String cmd = rootObj.getString("cmd");
				if("url".equals(cmd)){
					final String url = rootObj.getString("url");
					//WebSocketでメッセージが届いた
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "ws url:"+url, Toast.LENGTH_SHORT).show();
						}
					});
				}
				else if("txt".equals(cmd)){
					//テキストだった場合 音声合成してみる
					final String txt = rootObj.getString("txt");
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							aquesTalk2Util.speech(txt, speechResID, speechSpeed);
						}
					});
				}


			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onError(Exception e) {
			Log.d(TAG, "ws e:"+e.getMessage());
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			Log.d(TAG, "ws close:"+code+" "+reason+" "+remote);
		}
	};
}
