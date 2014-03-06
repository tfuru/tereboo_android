package biz.tereboo.tereboo.util;

import org.apache.http.client.CookieStore;

import android.content.Context;
import biz.tereboo.tereboo.http.HttpPostTask;

public class TerebooApiUtil {
	//APIのディレクトリ
	private static String API_URL = "http://tereboo.biz/tfuru/api/siritori.php";

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
}
