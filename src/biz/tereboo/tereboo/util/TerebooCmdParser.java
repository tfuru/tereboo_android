package biz.tereboo.tereboo.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

/** 音声認識コマンドのパーサ
 *
 * @author furukawanobuyuki
 *
 */
public class TerebooCmdParser {
	public static String RESULT_CMD = "CMD";
	public static String RESULT_TXT = "TXT";

	//コマンドヘッダー として認識する対象の文字列
	private static List<String> HEADERS = new ArrayList<String>(){
		{
			add("テレブー");
			add("セレブ");
			add("テレビ");
		}
	};

	//コマンド対象 コマンド内容として認識する対象文字列
	private static Map<String,String> COMMANDS = new HashMap<String, String>(){
		{
			put("ただいま", "tadaima");
			put("おやすみ", "oyasumi");

			put("これ買って","buy");

			//各放送局コマンド
			put("tbsをみせて", "tbs");
			put("tbsを見せて", "tbs");
			put("テレ東をみせて", "tvtokyo");
			put("テレ東を見せて", "tvtokyo");

			//
			put("しりとりをしようよ", "shiritori_start");
			put("しりとりをやろうよ", "shiritori_start");
			put("しりとりは終わり", "shiritori_stop");
			put("しりとりはおわり", "shiritori_stop");
		}
	};

	/** コマンド解析
	 *
	 * @param txt
	 */
	public static Map<String,String> parse(List<String> results){
		String param = null;
		String txt = null;
        for(String recognized : results) {
            //TODO 全部 ひらがな に変換してから ?
        	Log.d("onResults", recognized);
        	//ヘッダーが含まれているか確認
        	if(headerExists(recognized)){
        		//コマンド内容と一致する物を探す
        		param = commandExists(recognized);
        		if(param != null){
        			//レスポンス用の文字列
        			txt = recognized;
        			Log.d("onResults", param);
        			break;
        		}
        		else{
        			Log.d("onResults","is null");
        		}
        	}
        }

        //Result
        Map<String,String> result = new HashMap<String, String>();
        result.put(RESULT_CMD, param);
        result.put(RESULT_TXT, txt);

        return result;
	}

	/** ヘッダーが含まれているか確認
	 *
	 * @param recognized
	 * @return
	 */
	private static boolean headerExists(String recognized){
		boolean flg = false;
        for(String header:HEADERS){
        	int i = recognized.indexOf(header);
        	if(i != -1){
        		//コマンドヘッダーが見つかった
        		flg = true;
        	}
        }
        return flg;
	}

	/** コマンドが含まれているか調べる
	 *
	 * @param recognized
	 * @return
	 */
	private static String commandExists(String recognized){
		String result = null;
		for(Map.Entry<String, String> cmd : COMMANDS.entrySet()) {
        	int i = recognized.indexOf(cmd.getKey());
        	if(i != -1){
        		//コマンド内容が見つかった
        		result = cmd.getValue();
        	}
        }
        return result;
	}
}
