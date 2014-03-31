package biz.tereboo.client;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import bz.tereboo.client.R;

public class TutorialPagerAdapter extends PagerAdapter {
	private static final String TAG = TutorialPagerAdapter.class.getName();

	//コンテキスト
	private Activity activity;

	//ページ数
	private static int NUM_OF_VIEWS = 7;

	//住所のリストを生成
	private ArrayAdapter adapterPrefecture = null;

	/** コンストラクタ
	 *
	 * @param context
	 */
	public TutorialPagerAdapter(Activity activity){
		this.activity = activity;

		//住所のリストを生成
		this.adapterPrefecture = ArrayAdapter.createFromResource(activity, R.array.arrayPrefecture,
				android.R.layout.simple_spinner_item);
		this.adapterPrefecture.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
		int resId = this.activity.getResources().getIdentifier("activity_tutorial"+position,"layout",this.activity.getPackageName());
		view = inflater.inflate(resId, null);

    	if(lastPas == position){
    		//最後のページだった場合
    		Button btn = (Button)view.findViewById(R.id.btnNextMain);
    		btn.setOnClickListener(this.clickBtnFbLogin);
    	}
    	((ViewPager) collection).addView(view,0);

		//住所のリストを読み込む
		Spinner spinnerPrefecture = (Spinner) activity.findViewById(R.id.spinnerPrefecture);
		if(spinnerPrefecture != null){
			spinnerPrefecture.setAdapter(this.adapterPrefecture);
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

    	//ViewPagerに登録していたViewを削除する
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
