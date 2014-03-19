package biz.tereboo.tereboo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import bz.tereboo.tereboo.R;

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
	                //Intent intent = new Intent(SplashActivity.this, MainActivity.class);
	                Intent intent = new Intent(SplashActivity.this, TutorialActivity.class);
	                startActivity(intent);
	                finish();
	            }
	        }, 3000);
	   }
}
