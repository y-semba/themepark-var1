package environment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import setting.SystemConst;
import setting.ThemeParkGraph;

public class ThemePark implements Cloneable {
	/** ThemeParkの設定 */
	private final ThemeParkGraph themeParkGraph;
	private List<ThemeParkNode> themeParkNodes;
	private List<Visitor> visitors;
	
	private final ArrayList<Observer> observers = new ArrayList<>();
	
	private int entryCount = 0;
	private int exitCount = 0;
	private int simTime = 0;
	
	public ThemePark(ThemeParkGraph themeParkGraph, List<ThemeParkNode> themeParkNodes, List<Visitor> visitors) {
		this.themeParkGraph = themeParkGraph;
		this.themeParkNodes = themeParkNodes;
		this.visitors = visitors;
	}
	
	/**
	 * [0.0,1.0)のdouble乱数を発生させポアソン分布に従った乱数(k回入場)を発生
	 * entryCountがMAX_USERに到達するまで有効
	 */
	private void arriveVisitors() {
		double entval = SystemConst.ENT_RND.nextDouble();
		for (int i = 0; i < SystemConst.POISSON_DIS.length; i++) {
			if (entryCount == SystemConst.MAX_USER) return;
			if (entval < SystemConst.POISSON_DIS[i]) {
				break;
			}
			visitors.get(entryCount).enter();
			entryCount++;
		}
	}
	//WPSCE RNDを分けるために使う
	public void clonedArriveVisitors () {
		double entval = SystemConst.CLONED_ENT_RND.nextDouble();
		for (int i = 0; i < SystemConst.POISSON_DIS.length; i++) {
			if (entryCount == SystemConst.MAX_USER) return;
			if (entval < SystemConst.POISSON_DIS[i]) {
				break;
			}
			visitors.get(entryCount).enter();
			entryCount++;
		}
	}
	/** 入場済みのユーザが順番にplanSearch() */
	private void planVisitors() {
		switch (SystemConst.METHOD) {
		//WPSCEでは入場前のユーザも全体調整のためにプラン探索の必要
		//SCEなどでも全ユーザ探索する場合、visitor.planSearch()内で未入場ユーザのstatementを送信しないようにすれば正常に動くはず
		case WPSCE:
			for (int i = 0; i < SystemConst.MAX_USER; i++) {
				visitors.get(i).planSearch(this);
			}
			break;
			
		default:
			for (int i = 0; i < entryCount; i++) {
				visitors.get(i).planSearch(this);
			}
			break;
		}
	}
	
	/** 入場済みのユーザが順番に行動act() */
	public void actVisitors() {
		for (int i = 0; i < entryCount; i++) {
			visitors.get(i).act(this);
		}
	}
	/** 
	 * visitorから呼び出される関数.退場のcountをする.
	 * 毎回EnumStatus.TERMINATEDの人数を数えればテーマパーク側だけで完結はするが、処理無駄な気がするからこうしてる.
	 */
	public void exitVisitor() {
		exitCount++;
	}
	//WPSCE比較用 他の動作に影響はない
	public double calcResult() {
		double sumUtility = 0.0;
		for (Visitor visitor : visitors) {
			sumUtility += visitor.getUtility();
		}
		double avgUtility = sumUtility / visitors.size();
		System.out.println("avgUtility:" + avgUtility);
		System.out.println("--------------------------");
		return avgUtility;
	}
	
	/**
	 *  WPSCEで最新プランの受信として毎ステップ呼び出す
	 * statementも、この際に更新されるので入場済みユーザのみ適用(entryCount)
	 */
	private void receivePlanVisitors() {
		for (int i = 0; i < entryCount; i++) {
			visitors.get(i).receivePlan(this);
		}
	}
	/**
	 * WPSCEでは全体調整が入るためこちらを呼び出す
	 */
	public void simStepWPSCE() {
		arriveVisitors();
		planVisitors();
		if (simTime % SystemConst.SIM_PLANNING_INTERVAL == 0) {
			CentralServer.getInstance().simSearch(this);
		}
		receivePlanVisitors();
		notifyObservers();
		actVisitors();
		simTime++;
	}
	
	/**
	 * WPSCEでの全体調整の際のシミュレーションで呼ばれる
	 * プランの更新と、Observerへの通知などは行わない
	 */
	public void clonedSimStep() {
		clonedArriveVisitors();
		actVisitors();
		simTime++;
	}

	/** simulationの1step. 入場〜プランニング〜通知〜行動を入場済みユーザに対して行う */
	public void simStep() {
		arriveVisitors();
		planVisitors();
		notifyObservers();
		actVisitors();
		simTime++;
	}
	
	/** 
	 * (20210121) WPSCEではstepの処理が異なるので分岐を追加
	 * 全ユーザが退場するまでsimStep()を繰り返す.
	 * 最後に記録を取るため・終了処理としてendObservers()を呼び出す.
	 */
	public void sim() {
		switch (SystemConst.METHOD) {
			case WPSCE:
				//System.out.println("SimStart(t = " + simTime + ")");
				while(true) {
					simStepWPSCE();
					//System.out.println("Simstep(t = " + simTime + ")");
					if (exitCount == SystemConst.MAX_USER) {
						System.out.println("全ユーザが退場しました。");
						break;
					} else if (simTime == SystemConst.MAX_TIME) {
						System.out.println("simTimeが" + simTime + "stepに到達したため強制終了");
						break;
					}
				}
				calcResult();
				endObservers();
				break;

			default:
				//System.out.println("SimStart(t = " + simTime + ")");
				while(true) {
					simStep();
					//System.out.println("Simstep(t = " + simTime + ")");
					if (exitCount == SystemConst.MAX_USER) {
						System.out.println("全ユーザが退場しました。");
						break;
					} else if (simTime == SystemConst.MAX_TIME) {
						System.out.println("simTimeが" + simTime + "stepに到達したため強制終了");
						break;
					}
				}
				calcResult();
				endObservers();
				break;
		}
	}
	
	/** getter関連メソッド */
	public int getExitCount() {
		return exitCount;
	}
	public int getSimTime() {
		return simTime;
	}
	
	public ThemeParkGraph getThemeParkGraph() {
		return themeParkGraph;
	}
	
	public ThemeParkNode getNodeAt(int index) {
		return themeParkNodes.get(index);
	}
	
	public Visitor getVisitorAt(int index) {
		return visitors.get(index);
	}
	
	/** Observer関連メソッド */
	public void addObserver(Observer observer) {
        observers.add(observer);
    }
    public void deleteObserver(Observer observer) {
        observers.remove(observer);
    }
    public void notifyObservers() {
        Iterator<Observer> iterator = observers.iterator();
        while (iterator.hasNext()) {
            Observer observer = (Observer)iterator.next();
            observer.update(this);
        }
    }
    public void endObservers() {
        Iterator<Observer> iterator = observers.iterator();
        while (iterator.hasNext()) {
            Observer observer = (Observer)iterator.next();
            observer.end(this);
        }
    }
    public void calcEntryTime(List<Visitor> visitors) {
        int time = 0; // 現在の時間を追跡
        int i = 0; // 外部ループ用のインデックスを初期化

        // MAX_USER 分のエントリータイムを設定
        while (i < SystemConst.MAX_USER) {
            double entval = SystemConst.TP_RND.nextDouble(); // 乱数生成

            // ポアソン分布に基づいてエントリータイムを設定
            for (int j = 0; j < SystemConst.POISSON_DIS.length; j++) {
                if (entval < SystemConst.POISSON_DIS[j]) {
                    visitors.get(i).setEntryTime(time);
                    i++; // 次の訪問者に進む
                    break; // 条件が満たされたら次の訪問者の処理へ
                }
            }

            time += 1; // 時間を進める
        }
    }

    @Override
	public ThemePark clone() {
		ThemePark themePark = null;
		try {
			themePark = (ThemePark)super.clone();
			themePark.themeParkNodes = new ArrayList<ThemeParkNode>();
			for (ThemeParkNode themeParkNode : this.themeParkNodes) {
				themePark.themeParkNodes.add(themeParkNode.clone());
			}
			themePark.visitors = new ArrayList<Visitor>();
			for (Visitor visitor : this.visitors) {
				themePark.visitors.add(visitor.clone());
			}
			//observerはクローンしない
		} catch (Exception e) {
			e.printStackTrace();
		}
		return themePark;
	}
}
