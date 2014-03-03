package bz.tereboo.tereboo;

import java.util.List;

import bz.tereboo.tereboo.util.AquesTalk2Util;
import bz.tereboo.tereboo.util.SpeechRecognizerUtil;
import bz.tereboo.tereboo.util.TerebooCmdParser;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	//音声認識 ラッパークラス
	private SpeechRecognizerUtil speechRecognizerUtil = null;

	//音声合成 ラッパークラス
	private AquesTalk2Util aquesTalk2Util = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechRecognizerUtil = SpeechRecognizerUtil.getInstance(this,this.speechReclistener);
        aquesTalk2Util = AquesTalk2Util.getInstance(this);

        ((Button) findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//音声認 識開始
				speechRecognizerUtil.start();
				//aquesTalk2Util.speech("おかえり、てれびつけるね", R.raw.aq_yukkuri, 100);
			}
		});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private  SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener speechReclistener = new  SpeechRecognizerUtil.SpeechRecognizerUtilEventsListener(){

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
					aquesTalk2Util.speech("しりとり。はじめは、しりとりのりから。", R.raw.aq_yukkuri, 100);
					//Bluetooth で チャンネル切り替えを行う
				}
			}
			else{
				Toast.makeText(getApplicationContext(), "認識 コマンドを認識できませんでした", Toast.LENGTH_SHORT).show();
			}
		}

    };
}
