package biz.tereboo.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import biz.tereboo.client.util.CommonUtil;
import bz.tereboo.client.R;

public class SplashActivity extends Activity {

	   @Override
	   protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        setContentView(R.layout.activity_splash);

	        new Handler().postDelayed(new Runnable() {
	            @Override
	            public void run() {
	            	//TODO 初回起動時は Tutorial を表示
	            	boolean isFirstBoot = CommonUtil.GetBooleanSharedPreferences(getApplicationContext(),getString(R.string.PreferencesKeyIsFirstBoot));
	            	Intent intent = null;
	            	if(isFirstBoot == false){
	            		//初回起動時
	            		intent = new Intent(SplashActivity.this, TutorialActivity.class);
	                }
	            	else{
	            		//2回め以降
	            		intent = new Intent(SplashActivity.this, MainActivity.class);
	            	}
	                startActivity(intent);
	                finish();
	            }
	        }, 3000);
	   }
}
