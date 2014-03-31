package biz.tereboo.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import biz.tereboo.client.bluetooth.BluetoothUtil;
import biz.tereboo.client.util.AquesTalk2Util;
import android.os.Handler;

public class CommandQue {
	private static final String TAG = CommandQue.class.getName();

	//実行用のハンドラー
	private Handler mainHandler = new Handler();

	/** シングルスレッドExecutor
	 *
	 */
	private ExecutorService executorService;

	/** コンストラクタ　
	 *
	 */
	public CommandQue(){
		//シングルスレッドExecutor
		this.executorService = Executors.newSingleThreadExecutor();
	}

	/** コマンド実行
	 *
	 * @param command
	 */
	public synchronized void execute(CommandQueWork command){
		if( (this.executorService.isShutdown())
			 ||(this.executorService.isTerminated()) ) return;

		this.executorService.execute( command );
	}

	/** スレッドを停止
	 *
	 */
	public void shutdown(){
		this.executorService.shutdown();
	}

	/** スレッドで実行されるワーカークラス
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static class CommandQueWork implements Runnable {
		//Bluetooth ラッパークラス
		private BluetoothUtil bluetoothUtil = null;
		//音声合成 ラッパークラス
		private AquesTalk2Util aquesTalk2Util = null;

		private String txt;
		private int resID;
		private int speed;
		private Runnable work;

		//Bluetoothに送信するコマンド
		private static final String BLUETOOTH_CMD_START = "s start\n";
		private static final String BLUETOOTH_CMD_END   = "s end\n";

		public CommandQueWork(final BluetoothUtil bluetoothUtil,final AquesTalk2Util aquesTalk2Util,String txt,int resID,int speed){
			this.txt = txt;
			this.resID = resID;
			this.speed = speed;

			this.bluetoothUtil  = bluetoothUtil;
			this.aquesTalk2Util = aquesTalk2Util;
		}

		public CommandQueWork(final BluetoothUtil bluetoothUtil,final AquesTalk2Util aquesTalk2Util,String txt,int resID,int speed,final Runnable work){
			this.txt   = txt;
			this.resID = resID;
			this.speed = speed;
			this.work  = work;

			this.bluetoothUtil  = bluetoothUtil;
			this.aquesTalk2Util = aquesTalk2Util;
		}

		public void run() {
			//音声 開始
			bluetoothUtil.writeChatService(BLUETOOTH_CMD_START.getBytes());

			Runnable callback = new Runnable() {
				@Override
				public void run() {
					//音声 停止
					bluetoothUtil.writeChatService(BLUETOOTH_CMD_END.getBytes());

					if( CommandQueWork.this.work != null){
						CommandQueWork.this.work.run();
					}
				}
			};

			aquesTalk2Util.speech( this.txt, this.resID, this.speed, callback);
		}
	}
}

