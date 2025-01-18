package environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import setting.Graph;
import setting.SystemConst;

public class CentralServer {
	
	//各ユーザの候補プラン集合の直積
	private List<Map<Double, List<Integer>>> allPotentialAttOrders = null;
	//シミュレーション結果が最も優れたプランの組（探索後、ユーザのプラン受信で呼ばれる）
	private List<List<Integer>> adoptedAttOrders = null;
	
	/** Singleton */
	private static CentralServer centralServer = new CentralServer();
	
	private CentralServer() {
		allPotentialAttOrders = new ArrayList<>();
		for (int i = 0; i < SystemConst.MAX_USER; i++) {
			allPotentialAttOrders.add(new HashMap<>());
		}
	}
	public static CentralServer getInstance() {
		return centralServer;
	}
	
	/** getter */
	public List<Map<Double, List<Integer>>> getAllPotentialAttOrders(){
		return allPotentialAttOrders;
	}
	public List<List<Integer>> getAdoptedAttOrders(){
		return adoptedAttOrders;
	}
	
	/**
	 * 各ユーザのプラン集合を集約する
	 * @param device 呼び出し元device
	 * @param potentialAttOrders deviceで探索したプランの候補集合
	 */
	public void receiveAttOrders(Device device, Map<Double, List<Integer>> potentialAttOrders) {
		Map<Double, List<Integer>> attOrders = new HashMap<>(potentialAttOrders);
		allPotentialAttOrders.set(device.ownerId, attOrders);
	}
	
	/**
	 * 決定している全ユーザの訪問順序の組から、各ユーザjごとに訪問順序を確率SIM_SEARCH_PROBで訪問順序候補集合のいずれかに変更する
	 * @param candidateAttOrders 現在採用している全ユーザの訪問順序の組
	 * @return 各ユーザjの訪問順を探索確率で候補本門集合の要素に変更した、近傍全ユーザ訪問順
	 */
	private List<List<Integer>> makeNeighborAttOrders(List<List<Integer>> candidateAttOrders) {
		List<List<Integer>> neighborAttOrders = new ArrayList<>(candidateAttOrders);
		for (int j = 0; j < SystemConst.MAX_USER; j++) {
			//ユーザjの訪問順候補集合を取得.訪問順候補が他にない場合は次のユーザへ.
			Map<Double, List<Integer>> attOrders_j = allPotentialAttOrders.get(j);
			if (attOrders_j.size() <= 1) { continue; }
			
			//探索確率SIM_SEARCH_PROBでユーザjの訪問順を変更する
			//keyを乱数で1つ取得しそのkeyがマップされているオブジェクト(List<Integer> attOrder_j)がjの近傍訪問順
			double dval = SystemConst.SIM_SEARCH_RND.nextDouble();
			if (dval < SystemConst.SIM_SEARCH_PROB) {
				List<Double> keys = new ArrayList<>(attOrders_j.keySet());
				int randIndex = SystemConst.SIM_SEARCH_RND.nextInt(keys.size());
				double key = keys.get(randIndex);
				List<Integer> neigjborOrder = attOrders_j.get(key);
				
				//候補訪問順とサイズが異なる場合は揃える（近傍プランの探索はユーザごとに行われるのでノードが減ってない可能性がある）
				List<Integer> candidateOrder = candidateAttOrders.get(j);
				if (candidateOrder.size() != neigjborOrder.size()) {
					neigjborOrder.retainAll(candidateOrder);
				}
				//jの訪問順を更新
				neighborAttOrders.set(j, neigjborOrder);
			}
		}
		return neighborAttOrders;
	}
	
	/**
	 * テーマパークを複製し仮想シミューレション評価を行う
	 * ユーザはプランを途中で変えることはない
	 * @param attOrders シミュレーションで評価する全ユーザのアトラクション訪問順の組
	 * @param tp 複製元のテーマパーク
	 * @return 全ユーザが退場した時の平均効用値
	 */
	private double simEval(List<List<Integer>> attOrders, ThemePark tp) {
		//ThemeParkを複製し、複製後の各Visitorに引数で渡したプランを設定
		ThemePark clonedThemePark = tp.clone();
		for (int i = 0; i < SystemConst.MAX_USER; i++) {
			List<Integer> evalAttOrder = attOrders.get(i);
			Visitor clonedVisitor = clonedThemePark.getVisitorAt(i);
			List<Integer> plan = Graph.allDijkstra(clonedThemePark.getThemeParkGraph(), evalAttOrder, clonedVisitor.getPosition(), SystemConst.GRAPH_SIZE);
			clonedVisitor.setPlan(plan);
		}
		//複製後のvisitorが退場するまでのシミュレーションを行う
		while(true) {
			clonedThemePark.clonedSimStep();
			if (clonedThemePark.getExitCount() == SystemConst.MAX_USER) {
				System.out.println("全cloneユーザが退場しました。");
				break;
			} else if (clonedThemePark.getSimTime() == SystemConst.MAX_TIME) {
				System.out.println("clonedSimTimeが" + clonedThemePark.getSimTime() + "stepに到達したため強制終了");
				break;
			}
		}
		return clonedThemePark.calcResult();
	}
	
	/**
	 * 
	 * @param tp
	 */
	@SuppressWarnings("unused")
	public void simSearch(ThemePark tp) {
		List<List<Integer>> candidateAttOrders = new ArrayList<>();
		for (int j = 0; j < SystemConst.MAX_USER; j++) {
			//各ユーザに対して<評価値><attOrder>の集合から初期解として最も評価値の高いキーを取得し、その訪問順を取得
			Map<Double, List<Integer>> attOrders_j = allPotentialAttOrders.get(j);
			Double maxKey = 0.0;
			List<Integer> firstAttOrder_j = new ArrayList<>();
			try {
				maxKey = Collections.max(attOrders_j.keySet());
				firstAttOrder_j = attOrders_j.get(maxKey);
			} catch (NoSuchElementException e) {
				maxKey =(double) Integer.MIN_VALUE;
			}
			
			//maxKeyのCANDIDATE_PERCENTAGE%までの上位プランを採用
			//A:100% (100~200)(-200~-400)（Mapから消す）
			Iterator<Double> it = attOrders_j.keySet().iterator();
			while (it.hasNext()) {
				Double key = it.next();
				double allowRange = 0.0;
				allowRange = maxKey * SystemConst.CANDIDATE_PERCENTAGE * 0.01;
				if (key < maxKey - Math.abs(allowRange)) {
					it.remove();
				}
			}
			
			//探索時との時間差により残りアトラクション数が異なる場合は合わせ、visitor_jの訪問順序を確定
			//attOrders={attOrder_0}{attorder_1}...{attOrder_N}
			if (tp.getVisitorAt(j).getAttractionToVisit().size() != firstAttOrder_j.size()) {
				firstAttOrder_j.retainAll(tp.getVisitorAt(j).getAttractionToVisit());
			}
			candidateAttOrders.add(firstAttOrder_j);
		}
		
		/** 焼きなまし法 */
		List<List<Integer>> bestAttOrders = new ArrayList<>(candidateAttOrders);
		
		if (SystemConst.SIM_SEARCH_TIMES > 1) {
			//全ユーザの候補訪問順序をシミュレーションで評価
			System.out.println("[1]cloned simulation start!");
			double cValue = simEval(candidateAttOrders, tp);
			
			/** 焼きなまし法 */
			double bestValue = cValue;
			double temperature = SystemConst.TEMPERATURE;
			double cool = SystemConst.COOL;
			
			//プラン集合の中からvisitor_jが一定確率でプランを変更し、できた新たなプランの集合を近傍解とする山登り法を実行
			for (int i = 0; i < SystemConst.SIM_SEARCH_TIMES - 1; i++) {
				System.out.println("[" + (i + 2) + "]cloned simulation start!");
				List<List<Integer>> neighborAttOrders = makeNeighborAttOrders(candidateAttOrders);
				if (neighborAttOrders.equals(candidateAttOrders)) continue;
				double nValue = simEval(neighborAttOrders, tp);
				if (nValue > bestValue) {
					bestAttOrders = neighborAttOrders;
					bestValue = nValue;
					System.out.println("------------------");
					System.out.println("| update best! |");
					System.out.println("------------------");
				}
				if (SystemConst.SA_RND.nextDouble() <= probability(cValue, nValue, temperature)) {
					candidateAttOrders = neighborAttOrders;
					cValue = nValue;
					System.out.println("------------------");
					System.out.println("| SA change next |");
					System.out.println("------------------");
				}
				temperature *= cool;
			}
		}
		adoptedAttOrders = bestAttOrders;
	}
	public double probability(double e1, double e2, double t) {
		if (e1 <= e2) {
			return 1;
		}
		return Math.pow(Math.E, -Math.abs(e1 - e2) / t);
	}
}