package biz.tereboo.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		 if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
			 Toast.makeText(context, "起動したのでサービスを開始します。", Toast.LENGTH_SHORT).show();

			 //
			 //context = context.getApplicationContext();
			 //Intent service = new Intent(context, SampleService.class);
			 //context.startService(service);
		 }
	}

}
