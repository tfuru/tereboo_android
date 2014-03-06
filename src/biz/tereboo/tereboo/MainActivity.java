package biz.tereboo.tereboo;

import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import bz.tereboo.tereboo.R;

public class MainActivity extends Activity{
	private static final String TAG = "MainActivity";

	//音声認識 ラッパークラス
	private SpeechRecognizerUtil speechRecognizerUtil = null;

	//音声合成 ラッパークラス
	private AquesTalk2Util aquesTalk2Util = null;

	//Bluetooth ラッパークラス
	private BluetoothUtil bluetoothUtil = null;

	private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    //しりとりモード
    private boolean shiritoriMode = false;
    private Runnable shiritoriCallback = null;
	private CookieStore shiritoriCookieStore = null;

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
				byte[] send = "tbs\n".getBytes();
				bluetoothUtil.writeChatService(send);
			}
		});

        ((Button) findViewById(R.id.button4)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				//HTTPでデータ送信
				String url = "http://tereboo.biz/tfuru/channel.php";
				HttpPostTask postTask = new HttpPostTask(getApplicationContext(), url, postHandler);
				postTask.addPostParam("type", "channel");
				postTask.addPostParam("cmd", "tbs");
				postTask.addPostParam("q", "テレブ tbsに変えて");
				postTask.execute();

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
        bluetoothUtil.closeChatService();
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
			// TODO Auto-generated method stub

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

			if(cmd != null){
				Toast.makeText(getApplicationContext(), "認識 コマンド:"+cmd, Toast.LENGTH_SHORT).show();
				//TOTD APIサーバーへ通知してレスポンスを貰う
				//TODO APIサーバからのレスポンスを音声合成
				if( "tadaima".equals(cmd) ){
					//Toast.makeText(getApplicationContext(), "speech", Toast.LENGTH_SHORT).show();
					aquesTalk2Util.speech("おかえり、てれびつけるね。", R.raw.aq_yukkuri, 100);

					//Bluetooth で チャンネル切り替えを行う
				}
				else if( "tbs".equals(cmd) ){
					aquesTalk2Util.speech("てぃーびーえすにするね。", R.raw.aq_yukkuri, 100);
					//Bluetooth で チャンネル切り替えを行う
				}
				else if( "tvtokyo".equals(cmd) ){
					aquesTalk2Util.speech("てれとうにするね。", R.raw.aq_yukkuri, 100);
					//Bluetooth で チャンネル切り替えを行う
				}
				else if( "shiritori_start".equals(cmd) ){
					// aquesTalk2Util.speech("しりとり。はじめは、しりとりの、りから。", R.raw.aq_yukkuri, 100);

					//しりとり モードに入る
					shiritoriMode = true;
					q = "しりとりをやろうよ";
					TerebooApiUtil.shiritori(getApplicationContext(), siritoriHandler, shiritoriCookieStore, q, true);
				}
			}
			else{
				Toast.makeText(getApplicationContext(), "認識 コマンドを認識できませんでした", Toast.LENGTH_SHORT).show();
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

				aquesTalk2Util.speech(yomi, R.raw.aq_yukkuri, 100, shiritoriCallback);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}

		@Override
		public void onPostFailed(int statusCode, String response) {
			Toast.makeText(getApplicationContext(), "Failed statusCode:"+statusCode, Toast.LENGTH_SHORT).show();
		}

	};
}
