package biz.tereboo.client;

import bz.tereboo.client.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class UseActivity extends Activity {
	private static final String TAG = UseActivity.class.getName();

	//PagerAdapterの設定
	private UsePagerAdapter pagerAdapter;
	private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use);

        //PagerAdapterを設定
        this.pagerAdapter = new UsePagerAdapter( this );
        this.viewPager = (ViewPager) findViewById(R.id.viewpagerUse);
        this.viewPager.setAdapter( this.pagerAdapter );
    }
}
