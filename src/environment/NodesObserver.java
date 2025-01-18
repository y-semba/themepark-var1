package environment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import setting.SystemConst;

public class NodesObserver implements Observer {

	private Date date;
	private String filename;
	private PrintWriter writer;
	
	public NodesObserver() {
		date = new Date();
		String title = SystemConst.METHOD + "-" + SystemConst.MAX_USER + "-Queue_" + "SEED(" + SystemConst.SIM_SEED + ")";
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
		makeItems(tp);
	}
	@Override
	public void end(ThemePark tp) {
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
	/** csvファイルの先頭行を作成. time, Att[1]~Att[10] */
	private void makeHeadline() {
		writer.print("simulationTime,");
		for(int i = 0; i < SystemConst.NUM_OF_ATTRACTION; i++) {
			writer.print("Attraction[" + (i+1) + "]" + ",");
		}
		writer.print("\n");
	}
	/**
	 * simulationTimeとAttractionのqueueLengthを記録していく
	 * @param tp 観測対象のテーマパークインスタンス
	 */
	private void makeItems(ThemePark tp) {
		int simT = tp.getSimTime();
		writer.print(simT + ",");
		for (int i = 0; i < SystemConst.NUM_OF_ATTRACTION; i++) {
			Attraction att = (Attraction) tp.getNodeAt(i+1);
			int queueLength = att.getQueueLength();
			writer.print(queueLength + ",");
		}	
		writer.print("\n");
	}
	/** ファイルの終了処理 */
	private void close() {
		writer.close();
		System.out.println(filename + "作成");
	}
}
