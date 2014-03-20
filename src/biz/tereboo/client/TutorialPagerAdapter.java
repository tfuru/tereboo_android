package biz.tereboo.client;

import biz.tereboo.client.facebook.FacebookUtil;
import biz.tereboo.client.facebook.FacebookUtilInterface;
import bz.tereboo.client.R;

import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TutorialPagerAdapter extends PagerAdapter {
	private static final String TAG = TutorialPagerAdapter.class.getName();

	//コンテキスト
	private Activity activity;

	//ページ数
	private static int NUM_OF_VIEWS = 5;

	/** コンストラクタ
	 *
	 * @param context
	 */
	public TutorialPagerAdapter(Activity activity){
		this.activity = activity;
	}

    @Override
    public int getCount() {
        //Pagerに登録したビューの数を返却
        return NUM_OF_VIEWS;
    }

    /**
     * ページを生成する
     * position番目のViewを生成し返却するために利用
     * @param container: 表示するViewのコンテナ
     * @param position : インスタンス生成位置
     * @return ページを格納しているコンテナを返却すること。サンプルのようにViewである必要は無い。
     */
    @Override
    public Object instantiateItem(View collection, int position) {
		Log.d(TAG, "instantiateItem:"+position);

		LayoutInflater inflater = LayoutInflater.from(this.activity);
		final int lastPas = NUM_OF_VIEWS-1;

    	View view = null;
    	if(lastPas == position){
    		//最後のページだった場合
    		view = inflater.inflate(R.layout.activity_tutorial4, null);
    		Button btn = (Button)view.findViewById(R.id.btnNextMain);
    		btn.setOnClickListener(this.clickBtnFbLogin);
        	((ViewPager) collection).addView(view,0);
    	}
    	else{
			//TODO 各説明ページのレイアウトを読み込む
    		view = inflater.inflate(R.layout.activity_tutorial0, null);
        	TextView tv = (TextView)view.findViewById(R.id.txtPageNo);
        	tv.setText("postion :" + position);
        	((ViewPager) collection).addView(view,0);
    	}
    	return view;
    }

    /**
     * ページを破棄する。
     * postion番目のViweを削除するために利用
     * @param container: 削除するViewのコンテナ
     * @param position : インスタンス削除位置
     * @param object   : instantiateItemメソッドで返却したオブジェクト
     */
    @Override
    public void destroyItem(View collection, int position, Object view) {
        Log.d(TAG, "destroyItem:"+position);

    	//ViewPagerに登録していたTextViewを削除する
        ((ViewPager) collection).removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        //表示するViewがコンテナに含まれているか判定する(表示処理のため)
        //objecthainstantiateItemメソッドで返却したオブジェクト。
        return view==((View)object);
    }

    /** Facebook ログイン
     *
     */
    private View.OnClickListener clickBtnFbLogin = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			//メイン画面へ
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
            activity.finish();
		}
    };

}
