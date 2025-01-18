package environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import setting.EnumStatus;
import setting.SystemConst;

public class Visitor implements Cloneable {
	/** deviceの切り替えでコスト推定方を入れ替える */
	private Device device;

	private int visitorId;
	private static int visitorCount = 0;
	private boolean isEntered = false;

	/** 状態変数act()の条件分岐 */
	private EnumStatus actStatus = EnumStatus.INACTIVE;

	/** time系 */
	private int startTime = 0;
	private int endTime = 0;
	private int[] waitingTimes = new int[SystemConst.NUM_ATT_TO_VISIT];
	private int maxWTime = Integer.MIN_VALUE;
	private int minWTime = Integer.MAX_VALUE;
	private int waitingTime = 0;
	private int movingTime = 0;
	private int travelTime = 0;
	private int remainingTime;
	private int entryTime = 0;

	/** 選好値と効用(LinkedHashMapは効用と選好のindexを保持するため) */
	private Map<String, Double> preferences = new LinkedHashMap<>();
	private double utility = 0;

	/**
	 * positionはThemeParkNodeで管理していないのは依存度を下げるため
	 * ThemeParkインスタンスにpositionを渡すことでNodeのメソッドを呼び出す
	 */
	private int position = 0;

	/** 未訪問アトラクションと行動プラン（ノードシーケンス） */
	private List<Integer> attractionToVisit;
	private List<Integer> plan;

	/** コンストラクタ 固有のIDを振り分ける */
	public Visitor() {
		visitorId = visitorCount;
		initVisitor();
		visitorCount++;
	}

	/** getter メソッド ~~~ */
	public boolean getIsEntered() {
		return isEntered;
	}
	public int getPosition() {
		return position;
	}
	public int getId() {
		return visitorId;
	}
	public EnumStatus getActStatus() {
		return actStatus;
	}
	public int[] getWaitingTimes() {
		return waitingTimes;
	}
	public int getMaxWTime() {
		return maxWTime;
	}
	public int getMinWTime() {
		return minWTime;
	}
	public int getwaitingTime() {
		return waitingTime;
	}
	public int getMovingTime() {
		return movingTime;
	}
	public int getTravelTime() {
		return travelTime;
	}
	public int getRemainigTime() {
		return remainingTime;
	}
	public int getEntryTime() {
		return entryTime;
	}
	public LinkedHashMap<String, Double> getPreferences(){
		return (LinkedHashMap<String, Double>) preferences;
	}
	public double getUtility() {
		return utility;
	}
	public List<Integer> getAttractionToVisit() {
		return attractionToVisit;
	}
	public List<Integer> getPlan(){
		return plan;
	}
	/** ~~~ getter メソッド */

	/** PWSCEでコスト推定する際に、プランの組をclonedVisitorに設定する */
	public void setPlan(List<Integer> plan) {
		this.plan = plan;
	}
	public void setEntryTime(int time) {
		this.entryTime = time;
	}

	/**
	 * act()で呼び出すprivate関数
	 * プランが示す次ノードにpositionを移動し、移動前のノードをプランから除く。
	 * 移動先アトラクションの場合はリストから消す
	 * @param tp ThemeParkのインスタンス
	 * @return 移動先のThemeParkNode
	 */
	private ThemeParkNode move(ThemePark tp) {
		position = plan.get(1);
		if (attractionToVisit.contains((Integer)position)) {
			attractionToVisit.remove((Integer)position);
		}
		plan.remove(0);
		return tp.getNodeAt(position);
	}

	/**
	 * 累計待ち時間が保存されているwaitingTimes[]を各アトラクションでの待ち時間に変換する
	 */
	public void calcWaitingTimes() {
		int wSum = waitingTimes[0];
		// 累計時間[i]から[i-1]までの累計待ち時間をひく
		for (int i = 1; i < SystemConst.NUM_ATT_TO_VISIT; i++) {
			if (waitingTimes[i] == 0) continue;
			waitingTimes[i] = waitingTimes[i] - wSum;
			wSum += waitingTimes[i];
		}
		for (int i = 0; i < waitingTimes.length; i++) {
			if (waitingTimes[i] > maxWTime) maxWTime = waitingTimes[i];
			if (waitingTimes[i] < minWTime) minWTime = waitingTimes[i];
		}
	}

	/**
	 * TODO utilityのindexとpreferencesのindexが自動で結びつくようになれば便利
	 * TODO Deviceにutilityをact()で定義して委譲した方が綺麗
	 * u_j = u_jk * p_jk
	 * preferencesは順番を保持するためにLinkedHashMapの必要がある
	 * @return　個人効用u_j
	 */
	public double calcUtility() {
		// indexの順番を変える場合はpreferencesの並びも変える必要がある
		// 効用関数の定義
		double[] utilities = new double[preferences.size()];
		utilities[0] = -waitingTime;
		utilities[1] = -movingTime;
		utilities[2] = -travelTime;
		utilities[3] = -(maxWTime - minWTime);
		utilities[4] = -(minWTime - maxWTime);

		// 効用関数と選好値の積を取り、個人効用の計算
		double utility = 0.0;
		int i = 0;
		for (String key : preferences.keySet()) {
			utility += utilities[i] * preferences.get(key);
			i++;
		}
		return utility;
	}

	/**
	 *
	 * @param tp
	 */
	public void act(ThemePark tp) {
		ThemeParkNode currentNode = tp.getNodeAt(position);
		switch(actStatus) {
		case INACTIVE:
			// 入口に出現したらスタート
			if (position == SystemConst.ENTRANCE) {
				startTime = tp.getSimTime();
				// 次ノードへ移動して前ノードを除く
				currentNode = move(tp);
				if (currentNode.hasEmpty()) {
					actStatus = EnumStatus.SERVED;
					remainingTime = currentNode.getServiceTime();
				} else {
					// 今回の設定では入口の隣接ノードはRoadノードなので起こり得ない
					//TODO アトラクションがありえるなら待ち行列に登録する
					System.exit(2);
				}
			}
			break;

		case WAITING:
			waitingTime++;
			if (currentNode.canServe(getId())) {
				int wIndex = waitingTimes.length - attractionToVisit.size() - 1;
				waitingTimes[wIndex] = waitingTime;

				actStatus = EnumStatus.SERVED;
				remainingTime = currentNode.getServiceTime();
			}
			break;

		case SERVED:
			remainingTime--;
			if (remainingTime == 0) {
				//TODO アトラクションの処理を別でやって更新した方がいいかも？間違ってたとしても1stepしかずれないから多分誤差
				currentNode.finishService();
				if (currentNode.getCapacity() == Integer.MAX_VALUE) {
					movingTime += currentNode.getServiceTime();
				}
				currentNode = move(tp);
				if (position == SystemConst.EXIT) {
					endTime = tp.getSimTime();
					travelTime = endTime - startTime;
					calcWaitingTimes();
					utility = calcUtility();
					actStatus = EnumStatus.TERMINATED;
					// System.out.println("VisitorID [" + visitorId + "] が退場");
					exit(tp);
				} else {
					// Roadノードorアトラクション待ち行列なし
					if (currentNode.hasEmpty()) {
						actStatus = EnumStatus.SERVED;
						remainingTime = currentNode.getServiceTime();
					} else {
						// アトラクションノード待ち行列あり
						((Attraction) currentNode).registerQueue(getId());
						actStatus = EnumStatus.WAITING;
					}
				}
			}
			break;

		case TERMINATED:
			break;
		}
	}

	/**
	 * 操作側(ThemePark.sim())から呼び出すメソッド
	 * 分布に従って入場
	 *
	 * initVisitor()入場時に実行
	 * */
	public void enter() {
		isEntered = true;
		if (visitorId == SystemConst.MAX_USER /2) {
			System.out.println("VisitorID [" + visitorId + "] が入場");
		} else if (visitorId == SystemConst.MAX_USER - 1) {
			System.out.println("VisitorID [" + visitorId + "] が入場");
		}
		// System.out.println("VisitorID [" + visitorId + "] が入場");
	}

	public void exit(ThemePark tp) {
		if (visitorId == SystemConst.MAX_USER /2) {
			System.out.println("VisitorID [" + visitorId + "] が退場");
		} else if (visitorId == SystemConst.MAX_USER - 1) {
			System.out.println("VisitorID [" + visitorId + "] が退場");
		}
		tp.exitVisitor();
	}

	/**
	 * 重複なしの乱数をListのshuffleで作成して訪問数分のサブリストを返す
	 */
	private List<Integer> setAttractionToVisit() {
		List<Integer> attList = new ArrayList<>();
		for (int i = 0; i < SystemConst.NUM_OF_ATTRACTION; i++) {
			attList.add(i + 1);
		}
		Collections.shuffle(attList, SystemConst.DECIDE_ATT_RND);
		return new ArrayList<>(attList.subList(0, SystemConst.NUM_ATT_TO_VISIT));
	}

	/**
	 * @TODO: 選好値の偏りを設定ファイルに移動
	 * 設定ファイルで設定した効用関数に対応した選好値を総和１の乱数で生成する.初期化で呼び出す.
	 * @return 効用関数名に対応したMap
	 */
	private LinkedHashMap<String, Double> setPreferences() {
		// 選好値の初期化
		Map<String, Double> preferences = new LinkedHashMap<>();
		preferences.put("WT", 0.0);
		preferences.put("MT", 0.0);
		preferences.put("TT", 0.0);
		preferences.put("smooth", 0.0);
		preferences.put("bias", 0.0);

		// 設定ファイルでセットしたSet<>数の乱数を作成
		int nPref = SystemConst.PREFERENCES.size();
		double[] dVal = new double[nPref];
		double sum = 0.0;
		for (int i = 0; i < nPref; i++) {
			dVal[i] = SystemConst.PREF_RND.nextDouble();
			sum += dVal[i];
		}
		// 乱数の総和が1.0になるように正規化した値を設定した効用に対する選好値に代入
		int i = 0;
		for (String key : SystemConst.PREFERENCES) {
			dVal[i] = dVal[i] / sum;
			preferences.put(key, dVal[i]);
			i++;
		}
		//ここから特別実験のために変更してる~~~~
		// (1:9)->(0.2,1.8)のようにする．総和が一定になるように用いる効用数を比にかけること
		double raito = 0.5;
		double wt = preferences.get("WT");
		double mt = preferences.get("MT");

		// WTとMTの新しい値の計算
		wt = wt * raito / (wt * raito + mt * (1.0 - raito));
		mt = mt * (1.0 - raito) / (wt * raito + mt * (1.0 - raito));

		// 合計が1になるように正規化
		double total = wt + mt;
		wt = wt / total;
		mt = mt / total;

		// 結果をpreferencesに再度設定
		preferences.put("WT", wt);
		preferences.put("MT", mt);
		//~~~~~~~~ここまで
		
		return (LinkedHashMap<String, Double>) preferences;
	}
	/**
	 * enter()で呼び出す
	 * strategyの切り替えと初期ノードなどの決定
	 * W-SCEの場合は選好度の設定なども行う
	 */
	public void initVisitor() {

		switch (SystemConst.METHOD) {
			case CCE:
				device = new CCEDevice(visitorId);
				attractionToVisit = setAttractionToVisit();

				// WSCEとの比較用で,なくても動作する
				preferences = setPreferences();
				break;

			case SCE:
				device = new SCEDevice(visitorId);
				attractionToVisit = setAttractionToVisit();

				// WSCEとの比較用で,なくても動作する
				preferences = setPreferences();
				break;

			case WSCE:
				device = new WSCEDevice(visitorId);
				attractionToVisit = setAttractionToVisit();
				preferences = setPreferences();
				break;

			case WPSCE:
				device = new WPSCEDevice(visitorId);
				attractionToVisit = setAttractionToVisit();
				preferences = setPreferences();
				break;

			default:
				System.exit(999);
				break;
		}
	}

	public void planSearch(ThemePark tp) {
		switch (SystemConst.METHOD) {
			case WPSCE:
				if (actStatus == EnumStatus.TERMINATED) return;
				// 入口にいる場合は未入場なので一定間隔で実行
				if (position == SystemConst.ENTRANCE) {
					if (tp.getSimTime() % SystemConst.VISITOR_PLANNING_INTERVAL == 0) {
						device.searchPlan(tp, this);
					}
				} else {
					// 入場後はユーザごと（初移動後即実行するように余剰1）
					if ((tp.getSimTime() - startTime) % SystemConst.VISITOR_PLANNING_INTERVAL == 1) {
						device.searchPlan(tp, this);
					}
				}
				break;

			default:
				if (actStatus == EnumStatus.TERMINATED) return;
				if (position == SystemConst.ENTRANCE || (tp.getSimTime() - startTime) % SystemConst.VISITOR_PLANNING_INTERVAL == 0) {
					plan = device.searchPlan(tp, this);
				}
				break;
		}
	}

	/**
	 * WPSCEで毎秒最新プランを受信する
	 * 未入場のユーザは実行しない(statementも送信されない（正常))
	 */
	public void receivePlan(ThemePark tp) {
		if (actStatus == EnumStatus.TERMINATED) return;
		// 入場時とINTERVALで実行するように変更
		// 呼び出されるまで新statementは送信できない

		if (actStatus == EnumStatus.INACTIVE || tp.getSimTime() % SystemConst.SIM_PLANNING_INTERVAL == 0) {
			plan = ((WPSCEDevice) device).receivePlan(tp, this);
		}
	}

	@Override
	public Visitor clone() {
		Visitor visitor = null;
		try {
			visitor = (Visitor)super.clone();
			//visitor.device = this.device.clone();
			visitor.waitingTimes = this.waitingTimes.clone();
			visitor.preferences = new LinkedHashMap<>(this.preferences);
			visitor.attractionToVisit = new ArrayList<>(this.attractionToVisit);
			// visitor.plan = new ArrayList<>(this.plan);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return visitor;
	}
}