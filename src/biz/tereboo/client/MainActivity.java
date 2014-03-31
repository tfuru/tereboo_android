package biz.tereboo.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.CookieStore;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import biz.tereboo.client.CommandQue.CommandQueWork;
import biz.tereboo.client.bluetooth.BluetoothUtil;
import biz.tereboo.client.bluetooth.BluetoothUtil.BluetoothUtilEventsListener;
import biz.tereboo.client.bluetooth.DeviceListActivity;
import biz.tereboo.client.facebook.FacebookUtil;
import biz.tereboo.client.facebook.FacebookUtilInterface;
import biz.tereboo.client.http.HttpPostTask;
import biz.tereboo.client.util.AquesTalk2Util;
import biz.tereboo.client.util.CommonUtil;
import biz.tereboo.client.util.ImageUtil;
import biz.tereboo.client.util.SpeechRecognizerUtil;
import biz.tereboo.client.util.TerebooApiUtil;
import biz.tereboo.client.util.TerebooCmdParser;
import biz.tereboo.client.websocket.WebSocketUtil;
import bz.tereboo.client.R;

import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity{
	private static final String TAG = MainActivity.class.getName();

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

	//Facebook関連処理のユーティリティ
	private FacebookUtil facebookUtil;

	//CommandQue キュー
	private CommandQue commandQue;

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

        //初回起動かのフラグを保存する
        CommonUtil.SaveSharedPreferences(getApplicationContext(),getString(R.string.PreferencesKeyIsFirstBoot),true);

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
        String url = getString(R.string.WebSocketServerURL);
        this.webSocketUtil = WebSocketUtil.getInstance(getApplicationContext(),url,this.webSocketEventsListener);
    	try{
    		this.webSocketUtil.connect();
    	}
    	catch(Exception e){
    		Log.e(TAG, e.getMessage());
    		Toast.makeText(getApplicationContext(), "サーバーとの接続に失敗しました。", Toast.LENGTH_SHORT).show();
    	}

        //WebViewを閉じる
        ((Button) findViewById(R.id.btnWebViewClose)).setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				//WebView を 隠す
 				((RelativeLayout)findViewById(R.id.webViewContainer1)).setVisibility(View.GONE);
 			}
 		});

        //チュートリアルを表示
        ((Button) findViewById(R.id.btnShowTutorial)).setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				MainActivity.this.showTutorialActivity();
 			}
 		});

        //使い方を表示
        ((Button) findViewById(R.id.btnShowUse)).setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 			    //使い方
 				MainActivity.this.showUseActivity();
 			}
 		});

        //コマンド実行キュー
        this.commandQue = new CommandQue();

        //Facebook関連を初期化する
        iniFacebook();

        //リストビューを初期化
        initLogListView();
    }

    @Override
    public void onStart(){
    	super.onStart();

    	//デバイス一覧を表示
    	showDeviceList();
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
    	Log.w(TAG, "onDestroy");

    	//コマンド実行を停止
    	if(this.commandQue != null){
    		this.commandQue.shutdown();
        	this.commandQue = null;
    	}

        // Bluetooth 接続を終了
        this.bluetoothUtil.closeChatService();
        this.bluetoothUtil = null;

        try{
        	//WebSocket 切断
        	this.webSocketUtil.close();
        	this.webSocketUtil = null;
        }catch(Exception e){
        	Log.w(TAG, e.getMessage());
        }

        //aquesTalk2Utilを終了
        this.aquesTalk2Util = null;

        //speechRecognizerUtil
        if(this.speechRecognizerUtil != null){
        	//音声認識処理を終了
        	this.speechRecognizerUtil.stop();
        	this.speechRecognizerUtil = null;
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

    /** チュートリアルを表示する
     *
     */
    private void showTutorialActivity(){
 		Intent intent = new Intent(this, TutorialActivity.class);
     	startActivity(intent);
     	finish();
    }

    /** 使い方を表示する
    *
    */
   private void showUseActivity(){
		Intent intent = new Intent(this, UseActivity.class);
    	startActivity(intent);
    	finish();
   }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	boolean ret = true;
    	switch (item.getItemId()) {
    	 case R.id.action_bluetooth_connect:
    		 //テレブーと接続 Bluetooth デバイス一覧を表示 & 接続
    		 showDeviceList();
 			 ret = true;
    		 break;
    	 case R.id.action_speech_rec_test:
    		 //音声認識テスト
			speechRecognizerUtil.start();
			ret = true;
			break;
    	 case R.id.action_tutorial:
    		 //チュートリアル
    	 	this.showTutorialActivity();
    		 break;
    	 case R.id.action_use:
    		 //使い方
    	 	 this.showUseActivity();
    		 break;
    	 }
        return ret;
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
				//コマンドをログに保存
				addLog(cmd);

				//Toast.makeText(getApplicationContext(), "認識 コマンド:"+cmd, Toast.LENGTH_SHORT).show();
				//TOTD APIサーバーへ通知してレスポンスを貰う
				//TODO APIサーバからのレスポンスを音声合成
				if( TerebooCmdParser.COMMAND_TADAIMA.equals(cmd) ){
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
				else if( TerebooCmdParser.COMMAND_OYASUMI.equals(cmd) ){
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
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_TVTOKYO.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_TVTOKYO;
					effectSpeech("てれとうにするね。", speechResID, speechSpeed);
					//Bluetooth で チャンネル切り替えを行う
					bluetoothUtil.writeChatService("c tvtokyo\n".getBytes());
				}
				else if( TerebooCmdParser.COMMAND_CHANNEL_FUJITV.equals(cmd) ){
					selectChannel = TerebooCmdParser.COMMAND_CHANNEL_FUJITV;
					effectSpeech("ふじてれびにするね。", speechResID, speechSpeed);
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
				else if( TerebooCmdParser.COMMAND_SHIRITORI_START.equals(cmd) ){
					//しりとり モードに入る
					shiritoriMode = true;
					q = TerebooCmdParser.TXT_SHIRITORI_START;
					TerebooApiUtil.shiritori(getApplicationContext(), siritoriHandler, shiritoriCookieStore, q, true);
				}
				else if( "zatudan_start".equals(cmd) ){
					//雑談 モードに入る
					zatudanMode = true;
					q = "ざつだんをしようよ";
					TerebooApiUtil.shiritori(getApplicationContext(), zatudanHandler, zatudanCookieStore, q, true);
				}
				else if( TerebooCmdParser.COMMAND_BUY.equals(cmd) ){
					//これ買って
					// サーバにPOSTして その時間の 現在チャンネルの番組の商品を探して 購入させる？
					//ダミーでBooBoのオークション情報をおくる
					buyCall("BooBo");
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


				//TODO ログ件数が3の倍数の場合 チャンネルシェアを擬似的に実行
				Log.d(TAG, "aisatuMode:"+aisatuMode+" zatudanMode:"+zatudanMode+" shiritoriMode:"+shiritoriMode);
				Log.d(TAG, "logs:"+logs.size());

				if((logs.size() %3 == 0)
						&& (aisatuMode == false)
						&& (zatudanMode == false)
						&& (shiritoriMode == false)){
					//30秒後にチャンネルを切り替える
					(new Timer()).schedule(new TimerTask(){
						public void run(){
							Log.d(TAG, "channelShare");
							channelShare("0","てぃーびーえす");
						}
					},4000);
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
    	super.onActivityResult(requestCode, resultCode, data);
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

   	  	//Facebookのログイン処理
   	  	Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
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
							//ログに通知されたテキストを追加
							addLog(url);

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
					//TODO 自分宛てのメッセージだった場合 だけ処理する
					final String uid = rootObj.getString("uid");

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
			String url = getString(R.string.WebSocketServerURL);
	        webSocketUtil = WebSocketUtil.getInstance(getApplicationContext(),url,webSocketEventsListener);
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
		this.logs.add(0,log);
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
					//TODO スペース等を削除する

					//おすすめの 番組があったので教える
					String txt = "おすすめわ、"+title+"です。みる？";

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
					String txt = "おすすめわ、"+title+"です。みる？";

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
					String txt = "おすすめわ、"+title+"です。みる？";

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

	/** エフェクト付きスピーチ
	 *
	 * @param txt
	 * @param resID
	 * @param speed
	 */
	public void effectSpeech(String txt,int resID,int speed){
		//effectSpeech( txt, resID, speed, null);
		this.commandQue.execute( new CommandQueWork(bluetoothUtil, aquesTalk2Util, txt, resID, speed) );
	}

	/** エフェクト付きスピーチ
	 *
	 * @param txt
	 * @param resID
	 * @param speed
	 * @param callback1
	 */
	public void effectSpeech(String txt,int resID,int speed,final Runnable callback){
		if(this.commandQue == null){
			Log.d(TAG, "commandQue is null");
			return;
		}

		this.commandQue.execute( new CommandQueWork(bluetoothUtil, aquesTalk2Util, txt, resID, speed, callback) );
	}

	//友人とのチャンネル共有
	private void channelShare(String uid,String txtSelectChannel){
		JSONObject kv = new JSONObject();
		try{
			kv.put("cmd", "channel");
			kv.put("uid", uid);
			kv.put("channel", selectChannel);
			kv.put("txt", "ともだちが、"+txtSelectChannel+"をみてるよ?みる？");
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
		webSocketUtil.send( kv.toString() );
	}

	//これ欲しいの擬似
	private void buyCall(String query){
		JSONObject kv = new JSONObject();
		try{
			kv.put("cmd", "url");
			kv.put("url", "http://tereboo.biz/mori/ya_take.php?query="+query);
			kv.put("txt", "しょうひんのじょうほうをおくったよ。");
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

	//Facebook関連の初期処理
	private void iniFacebook(){
		this.facebookUtil = FacebookUtil.getInstance();
		this.facebookUtil.setCallback( fbUtilInterface );

		//ログイン処理
		this.facebookUtil.logion( this );
	}

    private FbUtilInterface fbUtilInterface = new FbUtilInterface();
    private class FbUtilInterface implements FacebookUtilInterface{

		@Override
		public void CallbackFacebookUtilStatusCallback(Session session,
				SessionState state, Exception exception) {
				Log.d(TAG, "CallbackFacebookUtilStatusCallback");
				Toast.makeText(getApplicationContext(), "Facebook ログイン成功", Toast.LENGTH_SHORT).show();
				//ユーザーの情報を取得する
				facebookUtil.getUser();
		}

		@Override
		public void CallbackFacebookUtilGetUserCallback(GraphUser user,
				Response response) {
			Log.d(TAG, "CallbackFacebookUtilGetUserCallback");
			if(user == null) return;

			//ユーザーの情報を取得
			TextView txtFbUserName = (TextView)findViewById(R.id.txtFbUserName);
			txtFbUserName.setText( user.getUsername() );
			TextView txtFbName = (TextView)findViewById(R.id.txtFbName);
			txtFbName.setText( user.getName() );

			// ブロフィール画像
			String url = FacebookUtil.createFacebookPictureUrl(user.getId());
			ImageView imgAccountThumbnailImage = (ImageView) findViewById(R.id.imgProfile);
			ImageUtil.DownloadImage(MainActivity.this,imgAccountThumbnailImage, url);

			//Cover 画像
			try{
				String cover = ((JSONObject)response.getGraphObject().getProperty("cover")).getString("source");
				Log.d(TAG, "cover:"+cover);
			}catch(Exception e){

			}
		}
    };
}
