package biz.tereboo.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

public class ImageUtil {
	private static String TAG = "ImageUtil";

	// キャッシュディレクトリ
	private static String CACHE_DIR = "/images/";

	public static void DownloadImage(Context context,ImageView imageView, String url) {
		new DownloadImageTask(context,imageView).execute(url);
	}

	private static class DownloadImageTask extends
			AsyncTask<String, Void, Bitmap> {
		private ImageView bmImage;
		private Context context;

		public DownloadImageTask(Context context,ImageView bmImage) {
			this.context = context;
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String url = urls[0];
			// キャッシュを確認する
			Bitmap bmp = cacheFileExists(url);
			if (bmp == null) {
				// サーバにアクセスしてBMPを生成する
				try {
					InputStream in = new URL(url).openStream();
					bmp = BitmapFactory.decodeStream(in);
				} catch (Exception e) {
					Log.e("Error", e.getMessage());
					e.printStackTrace();
				}
				// キャッシュに保存
				cacheFileSave(url, bmp);
			}
			return bmp;
		}

		protected void onPostExecute(Bitmap result) {
			if((bmImage == null)||(result == null)) return;
			bmImage.setImageBitmap(result);
		}

		//キャッシュファイルのオブジェクトを作成
		private File cacheFile(String url){
			//ディレクトリ有無
			String cache_dir_path = this.context.getCacheDir().getPath()+CACHE_DIR;
			File dir = new File(cache_dir_path);
			if(!dir.exists()){
				boolean result = dir.mkdir();
			}
			//キャッシュファイル オブジェクト作成
			String path = cache_dir_path+this.md5(url);
			return new File(path);
		}

		// キャッシュ確認
		private Bitmap cacheFileExists(String url) {
			Bitmap bmp = null;
			try{
				File file = this.cacheFile(url);
				if(file.exists()){
					//キャッシュがあったのでそちらを使う
					//TODO 有効期限等を考える事
	    			FileInputStream fis = new FileInputStream(file);
	    			bmp = BitmapFactory.decodeStream(fis);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			return bmp;
		}

		// キャッシュファイルを保存
		private void cacheFileSave(String url, Bitmap bmp){
			File file = this.cacheFile(url);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				//TODO URLの拡張子を見て変更するべき
				bmp.compress(CompressFormat.JPEG, 100, fos);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private String md5(String s) {
		    try {
		        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
		        digest.update(s.getBytes());
		        byte messageDigest[] = digest.digest();
		        StringBuffer hexString = new StringBuffer();
		        for (int i=0; i<messageDigest.length; i++)
		            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
		        return hexString.toString();
		    } catch (NoSuchAlgorithmException e) {
		        e.printStackTrace();
		    }
		    return "";
		}
	}
}
