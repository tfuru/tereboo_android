package biz.tereboo.tereboo;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TutorialPagerAdapter extends PagerAdapter {
	private static final String TAG = TutorialPagerAdapter.class.getName();

	//コンテキスト
	private Context context;

	//ページ数
	private static int NUM_OF_VIEWS = 5;

	/** コンストラクタ
	 *
	 * @param context
	 */
	public TutorialPagerAdapter(Context context){
		this.context = context;
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

    	if(NUM_OF_VIEWS-1 == position){
    		//最後の画面
    		Button btn = new Button( this.context );
    		btn.setText("メイン画面へ");
    		((ViewPager) collection).addView(btn,0);
        	return btn;
    	}
    	else{
        	TextView tv = new TextView( this.context );
        	tv.setText("postion :" + position);
        	tv.setTextSize(30);
        	((ViewPager) collection).addView(tv,0);
        	return tv;
    	}
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
        //今回はTextViewなので以下の通りオブジェクト比較
        return view==((TextView)object);
    }
}
