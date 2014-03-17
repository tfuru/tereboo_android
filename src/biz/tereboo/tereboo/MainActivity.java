package biz.tereboo.tereboo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.CookieStore;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import biz.tereboo.tereboo.bluetooth.BluetoothUtil;
import biz.tereboo.tereboo.bluetooth.BluetoothUtil.BluetoothUtilEventsListener;
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

	//ユーザーID
	public String userID = "0";

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

	//あいさつモード
    private boolean aisatuMode = false;
    private Runnable aisatuCallback = null;
	private CookieStore aisatuCookieStore = null;

	//チャンネル 共有
	public boolean channelShareMode = false;
    private Runnable channelShareCallback = null;
	private CookieStore channelShareCookieStore = null;
	public String channelShareChannel = null;
	public String channelShareTxt = null;

	//いしだ カテゴリ1
	private CookieStore ishidaCookieStore = null;

	//会話のリソースID
	private static final int speechResID = R.raw.ar_f4;
	private static final int speechSpeed = 100;

	//アクションログ
	private ArrayAdapter<String> logAdapter;
	private List<String> logs = new ArrayList<String>();

	private int retryCnt = 0;
	private static int MaxRetryCnt = 2;

	//選択中のチャンネル
	private static String selectChannel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechRecognizerUtil = SpeechRecognizerUtil.getInstance(this,this.speechReclistener);
        aquesTalk2Util = AquesTalk2Util.getInstance(this);

        //Bluetoothの状態の確認
        bluetoothUtil = BluetoothUtil.getInstance(this,bluetoothUtilEventsListener);
        if(false == bluetoothUtil.isSupport()){
        	//Bluetooth 未対応端末
        	//TODO アラーを表示してアプリ終了
        	Log.i(TAG,"Bluetooth 未対応端末でした");
        	//Toast.makeText(getApplicationContext(), "Bluetooth 未対応端末のようです。", Toast.LENGTH_SHORT).show();
        	this.finish();
        	return;
        }

        if(false == bluetoothUtil.isEnabled()){
        	//BluetoothがONになっていないのでONにするように促す
        	bluetoothUtil.showEnableBluetoothDialog(this, REQUEST_ENABLE_BLUETOOTH);
        }

        //WebSocket
        this.webSocketUtil = WebSocketUtil.getInstance(getApplicationContext(),webSocketServerURL,this.webSocketEventsListener);
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

        //WebViewを閉じる
        ((Button) findViewById(R.id.btnWebViewClose)).setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				//WebView を 隠す
 				((RelativeLayout)findViewById(R.id.webViewContainer1)).setVisibility(View.GONE);
 			}
 		});

        //リストビューを初期化
        initLogListView();
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
        try{
        	//WebSocket 切断
        	this.webSocketUtil.close();
        }catch(Exception e){

        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //ユーザーID切り替え
        userID = ("0".equals(userID))?"1":"0";
        //Toast.makeText(getApplicationContext(), "userID:"+userID, Toast.LENGTH_SHORT).show();
        return false;
    }

    private SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener speechReclistener = new  SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener(){

		@Override
		public void onEndOfSpeech() {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "認識 終了", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onError(int error) {
			//リトライ回数を越えてしまった場合何もしない
			if(retryCnt > MaxRetryCnt){
				//Toast.makeText(getApplicationContext(), "リトライ 終了", Toast.LENGTH_SHORT).show();
				return;
			}

			//Toast.makeText(getApplicationContext(), "認識 エラー", Toast.LENGTH_SHORT).show();
			Runnable callback = new Runnable() {
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

			effectSpeech("んー？なに？", speechResID, speechSpeed-20, callback);
			retryCnt++;
		}

		@Override
		public void onResults(List<String> results) {
			//リトライ回数をResetする
			retryCnt = 0;

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

			//あいさつモード
			if(aisatuMode == true){
				//あいさつモード中
				if(TerebooCmdParser.COMMAND_AISATU_STOP.equals(cmd)){
					//あいさつモード終了
					// "テレブー あいさつはおわり" で終了
					aisatuMode = false;
					effectSpeech(TerebooCmdParser.TXT_AISATU_STOP, speechResID, speechSpeed);
				}
				return;
			}

			//チャンネル共有
			if(channelShareMode == true){
				//みる,みない で返事
				if( isChannelShareMiru(results) ){
					// チャンネルをかえるね
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//Bluetoothでチャンネル変更
							String tmp = "c "+channelShareChannel+"\n";
							bluetoothUtil.writeChatService( tmp.getBytes() );
						}
					};
					effectSpeech("ちゃんねるをかえるね", speechResID, speechSpeed, callback);
					channelShareMode = false;
					retryCnt = 0;
				}
				else if( isChannelShareMinai(results) ){
					//　みないのー。
					effectSpeech("みないんだー", speechResID, speechSpeed);
					channelShareMode = false;
					retryCnt = 0;
				}
				else{
					//リトライ回数を越えてしまった場合何もしない
					if(retryCnt > MaxRetryCnt){
						//Toast.makeText(getApplicationContext(), "リトライ 終了", Toast.LENGTH_SHORT).show();
						return;
					}
					//ここで失敗した場合は リトライ
					Runnable callback = new Runnable() {
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
					effectSpeech("みる？", speechResID, speechSpeed, callback);
					retryCnt++;
				}

				return;
			}

			if(cmd != null){
				//Toast.makeText(getApplicationContext(), "認識 コマンド:"+cmd, Toast.LENGTH_SHORT).show();
				//TOTD APIサーバーへ通知してレスポンスを貰う
				//TODO APIサーバからのレスポンスを音声合成
				if( "tadaima".equals(cmd) ){
					//Toast.makeText(getApplicationContext(), "speech", Toast.LENGTH_SHORT).show();
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//BluetoothでテレビON
							bluetoothUtil.writeChatService("p on\n".getBytes());
						}
					};
					effectSpeech("おかえり、てれびつけるね。", speechResID, speechSpeed, callback);
				}
				else if( "oyasumi".equals(cmd) ){
					//Toast.makeText(getApplicationContext(), "speech", Toast.LENGTH_SHORT).show();
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//Bluetooth で テレビ OFF
							bluetoothUtil.writeChatService("p off\n".getBytes());
						}
					};
					effectSpeech("おやすみなさい。", speechResID, speechSpeed, callback);
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TBS.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_TBS;
					effectSpeech("てぃーびーえすにするね。", speechResID, speechSpeed);

					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tbs\n".getBytes());


					//チャンネルシェア
					channelShare("てぃーびーえす");
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TVTOKYO.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_TVTOKYO;
					effectSpeech("てれとうにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tvtokyo\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_FUJITV.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_FUJITV;
					effectSpeech("ふぃじてれびにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c fujitv\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TV_ASAHI.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_TV_ASAHI;
					effectSpeech("てれあさにするね。", speechResID, speechSpeed);

					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tv-asahi\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_NTV.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_NTV;
					effectSpeech("にってれにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c ntv\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_NHK.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_NHK;
					effectSpeech("えぬえちけーにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c nhk\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_E_TELE.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_E_TELE;
					effectSpeech("いーてれにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c e-tele\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_MXTV.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_MXTV;
					effectSpeech("えむえっくすにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c mxtv\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TELETAMA.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_TELETAMA;
					effectSpeech("てれたまにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c teletama\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_VOLUME_UP.equals(cmd) ){
					//Bluetooth で ボリュームをあげる
					effectSpeech("ぼりゅーむをあげるね。", speechResID, speechSpeed);
					bluetoothUtil.writeChatService("v up\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_VOLUME_DOWN.equals(cmd) ){
					//Bluetooth で ボリュームをさげる
					effectSpeech("ぼりゅーむをさげるね。", speechResID, speechSpeed);
					bluetoothUtil.writeChatService("v down\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_VOLUME_MUTE.equals(cmd) ){
					//Bluetooth で ボリュームをさげる
					effectSpeech("ぼりゅーむをみゅーとにするね。", speechResID, speechSpeed);
					bluetoothUtil.writeChatService("v mute\n".getBytes());
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
				else if( "drama".equals(cmd) ){
					// サーバにPOSTして その時間の ドラマを検索してチャンネル切り替え
					TerebooApiUtil.ishida_category2(getApplicationContext(), ishidaDramaHandler,"ドラマ");
				}
				else if( "sport".equals(cmd) ){
					// サーバにPOSTして その時間の スポーツ を検索してチャンネル切り替え
					TerebooApiUtil.ishida_category2(getApplicationContext(), ishidaSportHandler,"スポーツ");
				}
				else if( "news".equals(cmd) ){
					// サーバにPOSTして その時間の ニュース を検索してチャンネル切り替え
					TerebooApiUtil.ishida_category2(getApplicationContext(), ishidaNewsHandler,"ニュース");
				}
				else if( "golf".equals(cmd) ){
					// サーバにPOSTして その時間の ゴルフ を検索してチャンネル切り替え
					TerebooApiUtil.ishida_category2(getApplicationContext(), ishidaNewsHandler,"ゴルフ");
				}
				else if("livetter_hot".equals(cmd)){
					//人気番組名を取得
					TerebooApiUtil.livetter_hot(getApplicationContext(), livetterHotHandler);
				}
				else if( TerebooCmdParser.COMMAND_AISATU_START.equals(cmd)){
					//あいさつモード
					aisatuMode = true;
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//挨拶文を再生
							effectSpeech(TerebooCmdParser.TXT_AISATU_START, speechResID, speechSpeed-30);
						}
					};
					effectSpeech("はーい", speechResID, speechSpeed-20,callback);
				}
			}
			else{
				//コマンドじゃなかった ので 雑談APIになげてみる
				//Toast.makeText(getApplicationContext(), "認識 コマンドを認識できませんでした", Toast.LENGTH_SHORT).show();
				String txt = results.get(0);
				TerebooApiUtil.shiritori(getApplicationContext(), zatudanHandler, zatudanCookieStore, txt, true);
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

			//Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			// 失敗時レスポンス
			Log.d(TAG, "Failed statusCode:"+statusCode);
			//Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
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

				//Toast.makeText(getApplicationContext(), "yomi:"+yomi, Toast.LENGTH_SHORT).show();
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

			effectSpeech(yomi, speechResID, speechSpeed, shiritoriCallback);
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			//Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
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

				//Toast.makeText(getApplicationContext(), "yomi:"+yomi, Toast.LENGTH_SHORT).show();
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

			effectSpeech(yomi, speechResID, speechSpeed, zatudanCallback);
		}
		@Override
		public void onPostFailed(int statusCode, String response) {
			//Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
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
				//WebSocketでメッセージが届いた
				String cmd = rootObj.getString("cmd");
				if("url".equals(cmd)){
					final String txt = rootObj.getString("txt");
					final String url = rootObj.getString("url");

					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							//Toast.makeText(getApplicationContext(), "ws url:"+url, Toast.LENGTH_SHORT).show();
							//WebView で ページをを表示する
							((WebView)findViewById(R.id.webView1)).loadUrl(url);
							((RelativeLayout)findViewById(R.id.webViewContainer1)).setVisibility(RelativeLayout.VISIBLE);
						}
					});

					//通知されたテキストを話す
					effectSpeech(txt, speechResID, speechSpeed);
				}
				else if("txt".equals(cmd)){
					//テキストだった場合 音声合成してみる
					final String txt = rootObj.getString("txt");
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							effectSpeech(txt, speechResID, speechSpeed);
						}
					});
				}
				else if("channel".equals(cmd)){
					//自分宛てのメッセージだった場合
					final String uid = rootObj.getString("uid");
					if(userID.equals(uid) == false) return;
					//チャンネルだ共有 った場合 音声合成して番組をかえる
					final String channel = rootObj.getString("channel");
					//確認用のメッセージ
					final String txt = rootObj.getString("txt");
					//チャンネルシェアモードに入る
					channelShareMode = true;
					//channel txt
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//音声認 識開始
							speechRecognizerUtil.start();
						}
					};
					channelShareChannel = channel;
					effectSpeech(txt, speechResID, speechSpeed, callback);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onError(Exception e) {
			Log.d(TAG, "ws e:"+e.getMessage());
			//ここで再接続をしたい
			try{
				Thread.sleep(500);
			}catch(Exception ex){

			}

			Log.d(TAG, "webSocket 再接続");
			//再接続する
			webSocketUtil = null;
	        webSocketUtil = WebSocketUtil.getInstance(getApplicationContext(),webSocketServerURL,webSocketEventsListener);
			webSocketUtil.connect();
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			Log.d(TAG, "ws close:"+code+" "+reason+" "+remote);
		}
	};

	/** Bluetooth のリスナー
	 *
	 */
	public BluetoothUtilEventsListener bluetoothUtilEventsListener = new BluetoothUtilEventsListener(){

		@Override
		public void onMessage(byte[] message,int offset,int length) {
			//なに？の後音声認識
			String bleCmd = new String(message, offset, length);
			if(bleCmd.equals("j on\n")){
				//ジェスチャーを認識したので音声認識させる
				Runnable callback = new Runnable() {
					@Override
					public void run() {
						try{
							Thread.sleep(300);
						}catch(Exception e){

						}
						speechRecognizerUtil.start();
					}
				};
				if(true == aisatuMode){
					//あいさつモード
					effectSpeech(TerebooCmdParser.TXT_AISATU_START, speechResID, speechSpeed-30);
				}
				else{
					effectSpeech("なに？", speechResID, speechSpeed, callback);
				}
			}
		}

	};

	/** チャンネル共有 みる
	 *
	 * @param results
	 * @return
	 */
	private static boolean isChannelShareMiru(List<String> results){
		boolean flg = false;
		for(String l:results){
			if(l.equals("みる") || l.equals("見る")){
				flg = true;
				break;
			}
		}
		return flg;
	}

	/** チャンネル共有 みない
	 *
	 * @param results
	 * @return
	 */
	private static boolean isChannelShareMinai(List<String> results){
		boolean flg = false;
		for(String l:results){
			if(l.equals("みない") || l.equals("見ない")){
				flg = true;
				break;
			}
		}
		return flg;
	}

	/** ログリストを初期化
	 *
	 */
	private void initLogListView(){
		ListView lv = (ListView) findViewById(R.id.listView1);
		logAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, logs);

        lv.setAdapter(logAdapter);
	}

	/** ログ追加
	 *
	 * @param log
	 */
	private void addLog(String log){
		this.logs.add(log);
		//ListViewを更新
		logAdapter.notifyDataSetChanged();
	}

	/** いしだ スポーツ
	 *
	 */
	HttpPostTask.HttpPostHandler ishidaSportHandler = new HttpPostTask.HttpPostHandler(){

		@Override
		public void onPostSuccess(int statusCode, String response, CookieStore cookieStore) {
			try {
				JSONArray rootArray = new JSONArray(response);
				JSONObject obj = rootArray.getJSONObject(0);
				if(obj != null){
					//タイトルとチャンネルを取得
					String channel = obj.getString("channel");
					String title = obj.getString("title");

					//おすすめの 番組があったので教える
					String txt = "おすすめわ"+title+"です。みる？";

					//チャンネルシェアモードに入る
					channelShareMode = true;
					//channel txt
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//音声認 識開始
							speechRecognizerUtil.start();
						}
					};
					channelShareChannel = channel;
					effectSpeech(txt, speechResID, speechSpeed, callback);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			// TODO Auto-generated method stub

		}

	};

	/** いしだ ドラマ
	 *
	 */
	HttpPostTask.HttpPostHandler ishidaDramaHandler = new HttpPostTask.HttpPostHandler(){

		@Override
		public void onPostSuccess(int statusCode, String response, CookieStore cookieStore) {
			try {
				JSONArray rootArray = new JSONArray(response);
				JSONObject obj = rootArray.getJSONObject(0);
				if(obj != null){
					//タイトルとチャンネルを取得
					String channel = obj.getString("channel");
					String title = obj.getString("title");

					//おすすめの 番組があったので教える
					String txt = "おすすめわ"+title+"です。みる？";

					//チャンネルシェアモードに入る
					channelShareMode = true;
					//channel txt
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//音声認 識開始
							speechRecognizerUtil.start();
						}
					};
					channelShareChannel = channel;
					effectSpeech(txt, speechResID, speechSpeed, callback);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			// TODO Auto-generated method stub

		}

	};

	/** いしだ ニュース
	 *
	 */
	HttpPostTask.HttpPostHandler ishidaNewsHandler = new HttpPostTask.HttpPostHandler(){

		@Override
		public void onPostSuccess(int statusCode, String response, CookieStore cookieStore) {
			try {
				JSONArray rootArray = new JSONArray(response);
				JSONObject obj = rootArray.getJSONObject(0);
				if(obj != null){
					//タイトルとチャンネルを取得
					String channel = obj.getString("channel");
					String title = obj.getString("title");

					//おすすめの 番組があったので教える
					String txt = "おすすめわ"+title+"です。みる？";

					//チャンネルシェアモードに入る
					channelShareMode = true;
					//channel txt
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//音声認 識開始
							speechRecognizerUtil.start();
						}
					};
					channelShareChannel = channel;
					effectSpeech(txt, speechResID, speechSpeed, callback);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			// TODO Auto-generated method stub

		}

	};

    /** 人気番組を検索
     *
     */
	HttpPostTask.HttpPostHandler livetterHotHandler = new HttpPostTask.HttpPostHandler(){

		@Override
		public void onPostSuccess(int statusCode, String response, CookieStore cookieStore) {
			try {
				JSONObject obj = new JSONObject(response);
				if(obj != null){
					String channel = obj.getString("channel");
					String title = obj.getString("title");

					//おすすめの 番組があったので教える
					String txt = "にんきのばんぐみは、"+title+"です。みる？";

					//チャンネルシェアモードに入る
					channelShareMode = true;
					//channel txt
					Runnable callback = new Runnable() {
						@Override
						public void run() {
							//音声認 識開始
							speechRecognizerUtil.start();
						}
					};
					channelShareChannel = channel;
					effectSpeech(txt, speechResID, speechSpeed, callback);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			// TODO Auto-generated method stub

		}

	};

	//エフェクト付きスピーチ
	public void effectSpeech(String txt,int resID,int speed){
		//音声 開始
		bluetoothUtil.writeChatService("s start\n".getBytes());

		Runnable callback = new Runnable() {
			@Override
			public void run() {
				//音声 停止
				bluetoothUtil.writeChatService("s end\n".getBytes());
			}
		};
		aquesTalk2Util.speech(txt, speechResID, speechSpeed, callback);
	}

	public void effectSpeech(String txt,int resID,int speed,final Runnable callback1){
		Log.d(TAG, "txt:"+txt);

		//音声 開始
		bluetoothUtil.writeChatService("s start\n".getBytes());

		final Runnable callback = new Runnable() {
			@Override
			public void run() {
				//音声 停止
				bluetoothUtil.writeChatService("s end\n".getBytes());
				if(callback1 != null){
					callback1.run();
				}
			}
		};

		aquesTalk2Util.speech(txt, speechResID, speechSpeed, callback);
	}

	//友人とのチャンネル共有
	private void channelShare(String txtSelectChannel){
		if(userID == "1") return;

		JSONObject kv = new JSONObject();
		try{
			kv.put("cmd", "channel");
			kv.put("uid", (userID.equals("0"))?"1":"0");
			kv.put("channel", selectChannel);
			kv.put("txt", "ともだちが"+txtSelectChannel+"をみてるよ、いっしょに、みる？");
		}catch(Exception e){

		}
		webSocketUtil.send( kv.toString() );
	}

	/** あいさつを受け取る
	 *
	 */
	HttpPostTask.HttpPostHandler aisatuHandler = new HttpPostTask.HttpPostHandler(){
		@Override
		public void onPostSuccess(int statusCode, String response,CookieStore cookieStore) {
			//雑談の返事をJSONで受け取る
			try{
				//継続の為クッキーを保存
				aisatuCookieStore = cookieStore;

				JSONObject rootObj = new JSONObject(response);
				//String utt = rootObj.getString("utt");
				String yomi = rootObj.getString("yomi");

				//Toast.makeText(getApplicationContext(), "yomi:"+yomi, Toast.LENGTH_SHORT).show();
				Log.d(TAG, "aisatuMode:"+aisatuMode+" yomi:"+yomi);

				speech(yomi);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}

		//yomiの値を音声合成で出力する
		private void speech(String yomi){
			if(aisatuMode == true){
				//連続音声認識
				aisatuCallback = new Runnable() {
					@Override
					public void run() {
						if(aisatuMode == true){
							//連続音声認識
							Log.d(TAG, "speech callback");
							speechRecognizerUtil.start();
						}
					}
				};
			}

			effectSpeech(yomi, speechResID, speechSpeed, zatudanCallback);
		}
		@Override
		public void onPostFailed(int statusCode, String response) {
			//Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}
	};
}
