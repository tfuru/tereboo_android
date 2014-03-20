package biz.tereboo.client.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import bz.tereboo.client.R;

public class CommonUtil {
	private static String TAG = "CommonUtil";

	//現在の日付を取得
	public static String getNow(String format){
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		String strSysDate = sdf.format(calendar.getTime());
		return strSysDate;
	}

	//設定ファイルを開く
	private static SharedPreferences openSharedPreferences(Context context){
		String app_name = context.getString(R.string.app_name);
		SharedPreferences pref = context.getSharedPreferences(app_name, Context.MODE_PRIVATE);
		return pref;
	}

	//設定値 文字列を保存
	public static void SaveSharedPreferences(Context context,String key,String val){
		SharedPreferences pref = openSharedPreferences( context );
		Editor editor = pref.edit();
		editor.putString(key,val);
		editor.commit();
	}

	//設定値 文字列を保存
	public static void SaveSharedPreferences(Context context,String key,boolean val){
		SharedPreferences pref = openSharedPreferences( context );
		Editor editor = pref.edit();
		editor.putBoolean(key, val);
		editor.commit();
	}

	//設定値 文字列を読み込む
	public static String GetStringSharedPreferences(Context context,String key){
		SharedPreferences pref = openSharedPreferences( context );
		return pref.getString(key, null);
	}

	//設定値 boolean値を読み込む
	public static boolean GetBooleanSharedPreferences(Context context,String key){
		SharedPreferences pref = openSharedPreferences( context );
		return pref.getBoolean(key, false);
	}
}
