package biz.tereboo.tereboo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import bz.tereboo.tereboo.R;

public class TutorialActivity extends Activity {
	private static final String TAG = TutorialActivity.class.getName();

	//PagerAdapterの設定
	private TutorialPagerAdapter pagerAdapter;
	private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        //PagerAdapterを設定
        this.pagerAdapter = new TutorialPagerAdapter( getApplicationContext() );
        this.viewPager = (ViewPager) findViewById(R.id.viewpager01);
        this.viewPager.setAdapter( this.pagerAdapter );
    }
}
