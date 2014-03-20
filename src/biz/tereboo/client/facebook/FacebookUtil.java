package biz.tereboo.client.facebook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import biz.tereboo.client.MainActivity;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class FacebookUtil {
	private static final String TAG = FacebookUtil.class.getName();

	private static FacebookUtil instance = new FacebookUtil();
	private FacebookUtilInterface _callback;
	private Session _session;

	private FacebookUtil() {
	}

	public static FacebookUtil getInstance() {
		return instance;
	}

	public void setCallback(FacebookUtilInterface callback) {
		this._callback = callback;
	}

	private Session.StatusCallback _statusCallback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			if (session.isOpened()) {
				// セッションを保存しておく
				_session = session;
				// コールバック
				_callback.CallbackFacebookUtilStatusCallback(session, state,
						exception);
			}
		}
	};

	/**
	 * コールバック
	 *
	 * @param activity
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	public void CallbackOnActivityResult(Activity activity, int requestCode,
			int resultCode, Intent data) {
		Session.getActiveSession().onActivityResult(activity, requestCode,
				resultCode, data);
	}

	/**
	 * Facebook ログイン
	 *
	 */
	public void logion(Activity activity) {
		Session.openActiveSession(activity, true, _statusCallback);
	}

	/**
	 * Facebook ログアウト
	 *
	 * @param activity
	 */
	public void logout() {
		if (Session.getActiveSession() != null) {
			Session.getActiveSession().closeAndClearTokenInformation();
		}
		Session.setActiveSession(null);
	}

	/**
	 * ユーザ情報を取得
	 *
	 */
	public void getUser() {
		if(_session == null) return;

        Bundle params = new Bundle();
        params.putString("fields", "id,name,cover,friends.fields(id,name,link,username),link,username");
        params.putString("locale", "ja_JP");

        Request request = new Request(_session, "me", params, HttpMethod.GET, new Request.Callback(){
            @Override
            public void onCompleted(Response response){
            	Log.d(TAG, "onCompleted");
                if (response != null){
                	//コールバック
                	GraphUser user = response.getGraphObjectAs(GraphUser.class);
					_callback.CallbackFacebookUtilGetUserCallback(user, response);
                }
            }
        });
        Request.executeBatchAsync(request);
	}

	//Facebookのプロフィール画像URLを取得
	public static String createFacebookPictureUrl(final String facebookUserId){
		 return "https://graph.facebook.com/" + facebookUserId + "/picture?type=large";
	}
}
