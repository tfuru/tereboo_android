package biz.tereboo.client.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import biz.tereboo.client.util.AquesTalk2Util;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HttpPostTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = "HttpPostTask";

	private static final String ENCODING = "UTF-8";

	private static HttpPostTask instance;

	private Context context;

	// 送信先 URL
	private String postUrl;
	// UI等にレスポンスを通知するのハンドラー
	private Handler handler;

	private static CookieStore cookieStore = null;
	private DefaultHttpClient client = null;

	//送信パラメータ
	private List<NameValuePair> postParams = null;

	//レスポンスのステータスコード
	public int respStatusCode = 500;
	public String respData = null;

	public static String RESPONSE_STATUS_CODE = "statusCode";
	public static String RESPONSE_DATA = "responseData";

	/** コンストラクタ
	 *
	 * @param context
	 * @param postUrl
	 * @param handler
	 */
	public HttpPostTask(Context context, String postUrl,HttpPostTask.HttpPostHandler handler){
		this(context, postUrl, handler, null);
	}

	/** コンストラクタ
	 *
	 * @param context
	 * @param postUrl
	 * @param handler
	 * @param cookieStore
	 */
	public HttpPostTask(Context context, String postUrl,HttpPostTask.HttpPostHandler handler,CookieStore cookieStore){
		this.context = context;
		this.postUrl = postUrl;
		this.handler = handler;
		this.cookieStore = cookieStore;

		this.postParams =  new ArrayList<NameValuePair>();
	}

	/**
	 *
	 * @param post_name
	 * @param post_value
	 */
	public void addPostParam( String name, String value ){
		this.postParams.add(new BasicNameValuePair( name, value ));
	}

	/** 事前準備の処理
	 *
	 */
	@Override
	protected void onPreExecute() {

	}

	// HTTP レスポンスを受け取るハンドラー
	private ResponseHandler responseHandler = new ResponseHandler<Void>() {
		@Override
		public Void handleResponse(HttpResponse response) throws IOException {
			int statusCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "レスポンスコード："+statusCode);
			HttpPostTask.this.respStatusCode = statusCode;

			switch (statusCode) {
	        	case HttpStatus.SC_OK:
	        		//サーバとの接続 成功
	        		HttpPostTask.this.respData = EntityUtils.toString(response.getEntity(),HttpPostTask.ENCODING);
	        		break;
	        	case HttpStatus.SC_NOT_FOUND:
	        		//404
	        		HttpPostTask.this.respData = "NOT_FOUND";
	        		break;
	        	default:
	        		//何らかのエラー
	        		HttpPostTask.this.respData = "Error";
			}
			return null;
		}
	};

	/** メイン処理の実装 POST
  	*
  	*/
	@Override
	protected Void doInBackground(Void... params) {
		URI url = null;
		try {
			url = new URI(this.postUrl);
		}catch(Exception e){
			Log.i(TAG, "不正なURL");
			return null;
		}

		HttpPost request = new HttpPost( url );
		try {
		    // 送信パラメータのエンコードを指定
			request.setEntity(new UrlEncodedFormEntity(this.postParams, ENCODING));
		}
		catch (UnsupportedEncodingException e1) {
			return null;
		}

		// Cookie,SESSIONを利用するための設定
	    HttpContext localContext = new BasicHttpContext();
	    if(cookieStore != null){
	    	//クッキーの引き継ぎ
	    	localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	    }

		client = new DefaultHttpClient();
		try {
			client.execute(request, responseHandler, localContext);

			//接続後の CookieStoreを取得
			if(cookieStore == null){
				cookieStore = client.getCookieStore();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			Log.i(TAG, "プロトコルエラー");
		} catch (IOException e) {
			e.printStackTrace();
			Log.i(TAG, "IOエラー");
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "何らかのエラー");
		}

		return null;
	}

	/** タスク終了時
	 *
	 */
	@Override
	protected void onPostExecute(Void unused) {
		//httpクライアント終了処理
		client.getConnectionManager().shutdown();

		//UI等に通知するハンドラーを使って通知する
		Message message = new Message();
		Bundle bundle = new Bundle();
		bundle.putInt(RESPONSE_STATUS_CODE,respStatusCode);
		bundle.putString(RESPONSE_DATA,respData);

		message.setData(bundle);
		handler.sendMessage(message);
	}

	/** 通信結果を受け取るハンドラー
	 *
	 * @author furukawanobuyuki
	 *
	 */
	public static abstract class HttpPostHandler extends Handler{
		public void handleMessage(Message msg){
			Bundle bundle = msg.getData();
			int statusCode = bundle.getInt(RESPONSE_STATUS_CODE);
			String responseData = bundle.getString(RESPONSE_DATA);

			if(200 == statusCode){
				//成功時

				onPostSuccess(statusCode,responseData,cookieStore);
			}
			else{
				//失敗時のレスポンス
				onPostFailed(statusCode,responseData);
			}
		}
		//成功時のレスポンス通知
		public abstract void onPostSuccess(int statusCode, String response,CookieStore cookieStore);
		//失敗時のレスポンス通知
		public abstract void onPostFailed( int statusCode, String response );
	}
}
