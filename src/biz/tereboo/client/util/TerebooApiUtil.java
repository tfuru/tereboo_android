package biz.tereboo.client.util;

import org.apache.http.client.CookieStore;

import android.content.Context;
import biz.tereboo.client.http.HttpPostTask;

public class TerebooApiUtil {
	//APIのディレクトリ
	private static String API_URL = "http://tereboo.biz/tfuru/api/siritori.php";

	//いしだ カテゴリ1 (スポーツ)
	private static String API_URL_ISHIDA_CATEGORY1 = "http://tereboo.biz/ishida/searchcategory.php";

	//いしだ カテゴリ2
	private static String API_URL_ISHIDA_CATEGORY2 = "http://tereboo.biz/ishida/searchcategory2.php";

	//人気番組の名前を取得する
	private static String API_URL_LIVETTER_HOT ="http://tereboo.biz/tfuru/api/livetter_hot.php";

	/** しりとり
	 *
	 * @param context
	 * @param postHandler
	 * @param q
	 * @param start
	 */
	public static void shiritori(Context context,HttpPostTask.HttpPostHandler postHandler,CookieStore cookieStore,String q,boolean start){
		HttpPostTask httpPostTask = new HttpPostTask(context,API_URL,postHandler,cookieStore);
		httpPostTask.addPostParam("cmd", (start == true)?"start":"stop");
		httpPostTask.addPostParam("q", q);

		httpPostTask.execute();
	}

	/** いしだ Category1
	 *
	 * @param context
	 * @param postHandler
	 * @param cookieStore
	 * @param q
	 * @param start
	 */
	public static void ishida_category1(Context context,HttpPostTask.HttpPostHandler postHandler){
		HttpPostTask httpPostTask = new HttpPostTask(context,API_URL_ISHIDA_CATEGORY1,postHandler);
		httpPostTask.execute();
	}

	/** いしだ Category2 現在放送中の番組
	 *
	 * @param context
	 * @param postHandler
	 * @param cookieStore
	 * @param q
	 * @param start
	 */
	public static void ishida_category2(Context context,HttpPostTask.HttpPostHandler postHandler,String cate){
		HttpPostTask httpPostTask = new HttpPostTask(context,API_URL_ISHIDA_CATEGORY2,postHandler);
		httpPostTask.addPostParam("cate", cate);

		httpPostTask.execute();
	}

	/** 人気番組名を取得する
	 *
	 * @param context
	 * @param postHandler
	 */
	public static void livetter_hot(Context context,HttpPostTask.HttpPostHandler postHandler){
		HttpPostTask httpPostTask = new HttpPostTask(context,API_URL_LIVETTER_HOT,postHandler);
		httpPostTask.execute();
	}
}
