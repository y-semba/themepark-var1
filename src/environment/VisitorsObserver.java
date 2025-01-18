package environment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

import setting.SystemConst;

public class VisitorsObserver implements Observer {

	private Date date;
	private String filename;
	private PrintWriter writer;
	
	public VisitorsObserver() {
		date = new Date();
		String title = SystemConst.METHOD + "-" + SystemConst.MAX_USER + "_" + "SEED(" + SystemConst.SIM_SEED + ")";
		filename = SystemConst.FILE_PATH + title + ".csv";
		try {
			writer = new PrintWriter(new FileWriter(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
		makeTitle(date, title);
		makeHeadline();
	}
	
	@Override
	public void update(ThemePark tp) {
	}
	
	@Override
	public void end(ThemePark tp) {
		makeItems(tp);
		close();
	}
	/**
	 * 1,2行目に記載
	 * @param title ファイルのタイトル
	 * @param date ファイル作成日時
	 */
	private void makeTitle(Date date, String title) {
		writer.println(date);
		writer.println(title);
	}
	/** csvファイルの先頭行を作成. Visitorのフィールド */
	private void makeHeadline() {
		writer.print("VisitorId,");
		for(int i = 0; i < SystemConst.NUM_ATT_TO_VISIT; i++) {
			writer.print("waitingTime[" + i + "],");
		}
		writer.print("maxWTime,");
		writer.print("minWTime,");
		writer.print("sumWaitingTime,");	
		writer.print("movingTime,");
		writer.print("travelTime,");
		writer.print("preference[0](WT),");
		writer.print("preference[1](MT),");
		writer.print("preference[2](TT),");
		writer.print("preference[3](smooth),");
		writer.print("preference[4](bias),");
		writer.print("utility,");
		writer.print("\n");
	}
	/**
	 * sim()終了後に全visitorの情報を記録
	 * @param tp 観測対象のテーマパークインスタンス
	 */
	private void makeItems(ThemePark tp) {
		for (int i = 0; i < SystemConst.MAX_USER; i++) {
			Visitor visitor = tp.getVisitorAt(i);
			int visitorId = visitor.getId();
			int[] wTimes = visitor.getWaitingTimes();
			int maxWTime = visitor.getMaxWTime();
			int minWTime = visitor.getMinWTime();
			int sumWTime = visitor.getwaitingTime();
			int mTime = visitor.getMovingTime();
			int tTime = visitor.getTravelTime();
			Map<String, Double> preferences = visitor.getPreferences();
			double utility = visitor.getUtility();
			writer.print(visitorId + ",");
			for(int wTime : wTimes) {
				writer.print(wTime + ",");
			}
			writer.print(maxWTime + ",");
			writer.print(minWTime + ",");
			writer.print(sumWTime + ",");
			writer.print(mTime + ",");
			writer.print(tTime + ",");
			for (String key : preferences.keySet()) {
				writer.print(preferences.get(key) + ",");
			}
			writer.print(utility + ",");
			writer.print("\n");
		}
	}
	/** ファイルの終了処理 */
	private void close() {
		writer.close();
		System.out.println(filename + "作成");
	}
}
