package biz.tereboo.tereboo;

import java.util.List;

import biz.tereboo.tereboo.bluetooth.BluetoothChatService;
import biz.tereboo.tereboo.bluetooth.BluetoothUtil;
import biz.tereboo.tereboo.bluetooth.DeviceListActivity;
import biz.tereboo.tereboo.bluetooth.BluetoothUtil.BluetoothDevieFoundReceiverEventsListener;
import biz.tereboo.tereboo.util.AquesTalk2Util;
import biz.tereboo.tereboo.util.SpeechRecognizerUtil;
import biz.tereboo.tereboo.util.TerebooCmdParser;
import bz.tereboo.tereboo.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

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
				//aquesTalk2Util.speech("おかえり、てれびつけるね", R.raw.aq_yukkuri, 100);
			}
		});

        ((Button) findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Bluetooth デバイス一覧を表示
				showDeviceList();
			}
		});

        ((Button) findViewById(R.id.button3)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Bluetooth でデータ送信
				byte[] send = "tbs\r\n".getBytes();
				bluetoothUtil.writeChatService(send);
			}
		});
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
			String cmd = TerebooCmdParser.parse(results);
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
				else if( "shiritori".equals(cmd) ){
					aquesTalk2Util.speech("しりとり。はじめは、しりとりの、りから。", R.raw.aq_yukkuri, 100);
					//Bluetooth で チャンネル切り替えを行う
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
             case REQUEST_CONNECT_DEVICE_INSECURE:
                 if (resultCode == Activity.RESULT_OK) {
                	 bluetoothUtil.connectChatService(data, false);
                 }
                 break;
             case REQUEST_ENABLE_BT:
            	 break;
        	 }
         }
    }

    /** Bluetooth関連のイベントリスナー
     *
     */
    public BluetoothUtil.BluetoothDevieFoundReceiverEventsListener bluetoothDevieFoundReceiverEventsListener = new BluetoothUtil.BluetoothDevieFoundReceiverEventsListener(){

		@Override
		public void onDiscoveryStarted() {
            Log.i(TAG,"スキャン開始");
		}

		@Override
		public void onFound(BluetoothDevice foundDevice) {
			Log.i(TAG,"デバイスが検出された");
			String dName = foundDevice.getName();
			if(dName != null){
				Log.i(TAG,"dName:"+dName);
				//TODO ここで スピーカーと mbed の名前を探す？
			}
		}

		@Override
		public void onNameChanged(BluetoothDevice foundDevice) {
			Log.i(TAG,"デバイス名が検出された");
			String dName = foundDevice.getName();
			if(dName != null){
				Log.i(TAG,"dName:"+dName);
			}
		}

		@Override
		public void onDiscoveryFinished() {
			Log.i(TAG,"スキャン終了");
		}
    };
}
