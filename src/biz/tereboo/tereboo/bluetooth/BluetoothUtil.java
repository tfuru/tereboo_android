package biz.tereboo.tereboo.bluetooth;

import org.java_websocket.handshake.ServerHandshake;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import biz.tereboo.tereboo.util.AquesTalk2Util;

/** Bluetooth関連処理のラッパークラス
 *
 * @author furukawanobuyuki
 *
 */
public class BluetoothUtil {
	private static BluetoothUtil instance;
	private static Context context;

	private static BluetoothAdapter bluetoothAdapter;
	private static BluetoothDevieFoundReceiverEventsListener eventsListener;
	private static BluetoothChatService chatService = null;

	private static BluetoothUtilEventsListener listener;

	private BluetoothUtil(Context context,BluetoothUtilEventsListener listener){
		this.context = context;
		this.listener = listener;
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.chatService = new BluetoothChatService(context, chatServicehandler);
	}

	public static BluetoothUtil getInstance(Context context,BluetoothUtilEventsListener listener){
		if (instance == null) {
			synchronized(AquesTalk2Util.class) {
				instance = new BluetoothUtil(context,listener);
		    }
		}
		return instance;
	}

	/** Bluetooth対応端末かの確認
	 *
	 * @return
	 */
	public static boolean isSupport(){
		boolean flg = false;
		if(bluetoothAdapter != null){
			flg = true;
		}
		return flg;
	}

	/** BluetoothがONになっているか
	 *
	 * @return
	 */
	public static boolean isEnabled(){
		boolean flg = false;
		if(bluetoothAdapter != null){
			flg = bluetoothAdapter.isEnabled();
		}
		return flg;
	}

	/** BluetoothのをONにする確認ダイアログ
	 *
	 */
	public static void showEnableBluetoothDialog(Activity activity,int requestCode){
		Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		activity.startActivityForResult(btOn, requestCode);
	}

	/** 検索開始
	 *
	 */
	public static void startDiscovery(BluetoothDevieFoundReceiverEventsListener listener){
		//イベントリスナーを設定
		eventsListener = listener;

		//インテントフィルターとBroadcastReceiverの登録
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothDevieFoundReceiver, filter);

        if(bluetoothAdapter.isDiscovering()){
           	//検索を停止
        	bluetoothAdapter.cancelDiscovery();
        }

        //検索
        bluetoothAdapter.startDiscovery();
	}

	/** Bluetooth 機器検索時のブロードキャスト受け取り
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static BroadcastReceiver bluetoothDevieFoundReceiver = new BroadcastReceiver() {
		private static final String TAG = "BluetoothDevieFoundReceiver";

		@Override
		public void onReceive(Context context, Intent intent) {
			if(eventsListener == null) return;
			String action = intent.getAction();
			String dName = null;
			BluetoothDevice foundDevice;
			if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
	            eventsListener.onDiscoveryStarted();
	        }
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				//デバイスが検出された
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				eventsListener.onFound(foundDevice);
			}
			if(BluetoothDevice.ACTION_NAME_CHANGED.equals(action)){
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				eventsListener.onNameChanged(foundDevice);
			}
			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				eventsListener.onDiscoveryFinished();
	        }
		}
	};

	/** イベントリスナー
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static abstract class BluetoothDevieFoundReceiverEventsListener {
		public abstract void onDiscoveryStarted();
		public abstract void onFound(BluetoothDevice foundDevice);
		public abstract void onNameChanged(BluetoothDevice foundDevice);
		public abstract void onDiscoveryFinished();
	}

	/** 接続開始
	 *
	 */
	public static void startChatService(){
		if(chatService == null) return;
		chatService.start();
	}

	/** デバイスと接続
	 *
	 */
	public static void connectChatService(Intent data, boolean secure){
		if(chatService == null) return;
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        chatService.connect(device, secure);
	}

	/** 送信
	 *
	 * @param send
	 */
	public static void writeChatService(byte[] send){
		if(chatService == null) return;
        chatService.write(send);
	}

	public static void closeChatService(){
		if(chatService == null) return;
		chatService.stop();
	}

	/** Bluetooth
	 *
	 */
    private static final Handler chatServicehandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothChatService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
	                case BluetoothChatService.STATE_CONNECTED:
	                    //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
	                	//mConversationArrayAdapter.clear();
	                    break;
	                case BluetoothChatService.STATE_CONNECTING:
	                    //setStatus(R.string.title_connecting);
	                    break;
	                case BluetoothChatService.STATE_LISTEN:
	                case BluetoothChatService.STATE_NONE:
	                    //setStatus(R.string.title_not_connected);
	                    break;
                }
                break;
            case BluetoothChatService.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String(writeBuf);
                Toast.makeText(context, "writeMessage "+ writeMessage, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothChatService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                Toast.makeText(context, "readMessage "+ readMessage, Toast.LENGTH_SHORT).show();

                //リスナー通知
                listener.onMessage(readBuf, 0, msg.arg1);

                break;
            case BluetoothChatService.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                String name = msg.getData().getString(BluetoothChatService.DEVICE_NAME);
                Toast.makeText(context, "Connected to "+ name, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothChatService.MESSAGE_TOAST:
                Toast.makeText(context, msg.getData().getString(BluetoothChatService.TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
   };

	/** イベントリスナー
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static abstract class BluetoothUtilEventsListener {
		//メッセージが届いた
		public abstract void onMessage(byte[] message,int offset,int length);
	}
}
