package environment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import setting.SystemConst;

public class PlansSetObserver implements Observer {

	private Date date;
	private String filename;
	private PrintWriter writer;
	
	public PlansSetObserver() {
		date = new Date();
		String title = SystemConst.METHOD + "-" + SystemConst.MAX_USER + "-PlansSet_" + "SEED(" + SystemConst.SIM_SEED + ")";
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
		if (tp.getSimTime() % SystemConst.SIM_PLANNING_INTERVAL == 0) {
			makeItems(tp);
		}
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
	/** csvファイルの先頭行を作成. time, Visitor[0]~Visitor[MAX] */
	private void makeHeadline() {
		writer.print("simulationTime,");
		for(int i = 0; i < SystemConst.MAX_USER; i++) {
			writer.print("Visitor[" + i + "]" + ",");
		}
		writer.print("\n");
	}
	/**
	 * 全体調整タイミングのallowPlanの数を記録していく
	 * @param tp 観測対象のテーマパークインスタンス
	 */
	private void makeItems(ThemePark tp) {
		int simT = tp.getSimTime();
		writer.print(simT + ",");
		CentralServer centralServer = CentralServer.getInstance();
		List<Map<Double, List<Integer>>> allPotentialAttOrders = centralServer.getAllPotentialAttOrders();
		for (int i = 0; i < allPotentialAttOrders.size(); i++) {
			Map<Double, List<Integer>> attOrders_j = allPotentialAttOrders.get(i);
			int nOfPlans = attOrders_j.size();
			writer.print(nOfPlans + ",");
		}	
		writer.print("\n");
	}
	/** ファイルの終了処理 */
	private void close() {
		writer.close();
		System.out.println(filename + "作成");
	}
}
