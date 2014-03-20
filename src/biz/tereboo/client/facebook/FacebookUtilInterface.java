package biz.tereboo.client.facebook;

import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public interface FacebookUtilInterface {
	public void CallbackFacebookUtilStatusCallback(Session session, SessionState state, Exception exception);
	public void CallbackFacebookUtilGetUserCallback(GraphUser user,Response response);
}
