package environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class Device {
	protected int ownerId;
	protected List<Integer> ETA = new ArrayList<>();
	protected Random planRnd;
	
	public Device(int userId) {
		ownerId = userId;
		planRnd = new Random(ownerId);
	}
	/**
	 * evalPlan()の内部で繰り返し呼び出される。単体で使うことはない
	 * @param nodeId 通過するコストを計算するノードのid
	 * @param time ノードのコスト計算時点での到達予測時刻time^*
	 * @param tp テーマパークインスタンス
	 * @param visitor 呼び出しユーザ
	 * @return Node[nodeId]を通過するのにかかるコスト
	 */
	public abstract int costEstimate(int nodeId, int time, ThemePark tp, Visitor visitor);
	
	/**
	 * TODO W-SCEなら内部で予測移動時間などの要素を別で計算してutilityを演算して返す
	 * 
	 * searchPlan()内で呼び出される。
	 * 終点ノードまでのETA[]をcostEstimate()を内部で呼び出して計算する。
	 * @param plan 評価対象のプラン
	 * @param tp テーマパークインスタンス
	 * @param visitor 呼び出しユーザ
	 * @return planの評価値(CCE,SCEの場合は滞在時間)
	 */
	public abstract double evalPlan(List<Integer> plan, ThemePark tp, Visitor visitor);
	
	/**
	 * CCE: ローカルサーチでプランを探索
	 * @param tp テーマパークインスタンス
	 * @param visitor 利用ユーザ
	 * @return 最も優れたプランのList<Integer>
	 */
	public abstract List<Integer> searchPlan(ThemePark tp, Visitor visitor);
	
	/**
	 * returnするOrderとうけとるOrderは別インスタンス(参照わたしじゃない)
	 * @param targetAttraction 変更前の元のアトラクション訪問順
	 * @return 引数の訪問順のうち2点を入れ替えたList
	 */
	public List<Integer> swapTwoPoints(List<Integer> targetAttraction) {
		List<Integer> attNewOrder = new ArrayList<>(targetAttraction);
		//残りアトラクション2点未満の場合は入れ替えなく元のリストを返す
		if (targetAttraction.size() < 2)
			return targetAttraction;
		
		//残りアトラクションの数の重複なし乱数を取得して入れ替え
		List<Integer> rndList = new ArrayList<>();
		for (int i = 0; i < targetAttraction.size(); i++) {
			rndList.add(i);
		}
		Collections.shuffle(rndList, planRnd);
		Integer index1 = rndList.get(0);
		Integer index2 = rndList.get(1);
		Integer tmp = attNewOrder.get(index1);
		attNewOrder.set(index1, attNewOrder.get(index2));
		attNewOrder.set(index2, tmp);
		return attNewOrder;
	}
}