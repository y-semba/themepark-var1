package environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import setting.EnumStatus;
import setting.Graph;
import setting.SystemConst;

public class WPSCEDevice extends Device {
	
	//全体調整を行うサーバインスタンス（Singleton）
	private CentralServer centralServer = null;
	
	//visitor_jのプラン候補集合
	private Map<Double, List<Integer>> potentialAttOrders = null;
	
	private StatementServer statementServer = null;
	List<Statement> statement_j = new ArrayList<>();
	
	public WPSCEDevice(int userId) {
		super(userId);
		statementServer = StatementServer.getInstance();
		centralServer = CentralServer.getInstance();
		potentialAttOrders = new HashMap<>();
	}

	@Override
	public int costEstimate(int nodeId, int time, ThemePark tp, Visitor visitor) {
		if (time == tp.getSimTime()) {
			if (nodeId == SystemConst.ENTRANCE ) {
				return 0; 
			}
			//入口じゃなく、INACTIVEになるのは初期入場時のplan[1]のRoadのみ
			if (visitor.getActStatus() == EnumStatus.INACTIVE) {
				return tp.getNodeAt(nodeId).getServiceTime();
			} else if (visitor.getActStatus() == EnumStatus.SERVED) {
				return visitor.getRemainigTime();
			} else {
				Attraction attraction = ((Attraction)tp.getNodeAt(nodeId));
				int pQueue = attraction.getPriorQueueLength(ownerId);
				int window = attraction.getCapacity();
				int serviceTime = attraction.getServiceTime();
				return (pQueue / window + 1) * serviceTime;
			}
		}
		
		ThemeParkNode currentNode = tp.getNodeAt(nodeId);
		//Roadの場合のコストは単にst_iになる
		if (currentNode.getCapacity() == Integer.MAX_VALUE) {
			return currentNode.getServiceTime();
		} else {
			int cQueue = ((Attraction)currentNode).getQueueLength();
			int window = ((Attraction)currentNode).getCapacity();
			int serviceTime = ((Attraction)currentNode).getServiceTime();
			int expectedArrivals = getArrivalsUntil(currentNode.getNodeId(), tp.getSimTime(), time);
			int num = cQueue + expectedArrivals;
			int fQueue = Math.max(0, num - (time - tp.getSimTime()) * window / serviceTime);
			
			return (fQueue / window + 1) * serviceTime;
		}
	}

	@Override
	public double evalPlan(List<Integer> plan, ThemePark tp, Visitor visitor) {
		ETA.clear();
		if (visitor.getIsEntered() == false) {
		    ETA.add(visitor.getEntryTime()); //未入場者には入場予定時刻を割り当て
		} else {
		    ETA.add(tp.getSimTime()); //入場済みの人には現在の時間を割り当て
		}
		for (int i = 1; i < plan.size(); i++) {
			ETA.add(ETA.get(i - 1) + costEstimate(plan.get(i-1), ETA.get(i - 1), tp, visitor));
		}
		
		int estTTime = ETA.get(plan.size() - 1) - ETA.get(0);
		int estWTime = 0;
		int estMTime = 0;
		int estMaxWTime = Integer.MIN_VALUE;
		int estMinWTime = Integer.MAX_VALUE;
		for (int i = 0; i < plan.size() - 1; i++) {
			int nodeId = plan.get(i);
			int diffTime = ETA.get(i + 1) - ETA.get(i);
			if (nodeId >= 11 && nodeId <= 19) {
				estMTime += diffTime;
			} else if (nodeId >=1 && nodeId <=10) {
				int diffWT = Math.max(0, diffTime - tp.getNodeAt(nodeId).getServiceTime());
				estWTime += diffWT;
				if (diffWT > estMaxWTime) estMaxWTime = diffWT;
				if (diffWT < estMinWTime) estMinWTime = diffWT;
			} else if (nodeId == 0) {
				//入口なら何もしない
			} else {
				System.out.println("W-SCEでバグ発生");
				System.exit(777);
			}
		}
		double[] estUtilities = new double[visitor.getPreferences().size()];
		estUtilities[0] = -estWTime;
		estUtilities[1] = -estMTime;
		estUtilities[2] = -estTTime;
		estUtilities[3] = -(estMaxWTime - estMinWTime);
		estUtilities[4] = -(estMinWTime - estMaxWTime);
		
		return calcUtility(estUtilities, visitor.getPreferences());
	}
	
	public double calcUtility(double[] utilities, LinkedHashMap<String, Double> preferences) {
		//効用関数と選好値の積を取り、個人効用の計算
		double utility = 0.0;
		int i = 0;
		for (String key : preferences.keySet()) {
			utility += utilities[i] * preferences.get(key);
			i++;
		}
		return utility;
	}
	
	@Override
	public List<Integer> searchPlan(ThemePark tp, Visitor visitor) {
		//statementはプラン受信時にのみ更新（プラン探索時に自身のstatementを除くため一時的に削除）
		deleteStatement();
		
		//候補プランとして初期プランの決定と評価値を計算
		List<Integer> candidateAttOrder = new ArrayList<>(visitor.getAttractionToVisit());
		List<Integer> candidatePlan = Graph.allDijkstra(tp.getThemeParkGraph(), candidateAttOrder, visitor.getPosition(), SystemConst.GRAPH_SIZE);
		double cPlanEval = evalPlan(candidatePlan, tp, visitor);
		
		//WPSCE
		potentialAttOrders.clear();
		potentialAttOrders.put(cPlanEval, candidateAttOrder);
		
		// 山登り法の繰り返しをカウント. 残り2点: プラン数2!=2, 残り3点: 3!=6.
		//(20230116) if (candidateAttOrder.size() == 3) searchCount += 7;は結果が変わる
		int searchCount = 0;
		if (candidateAttOrder.size() == 2) searchCount += 14;
		while (true) {
			//2点を入れ替えた近傍プランの作成と評価
			List<Integer> neighborAttOrder = swapTwoPoints(candidateAttOrder);
			
			//残りアトラクションが1点以下の場合は探索終了
			if(neighborAttOrder.equals(candidateAttOrder)) {
				break;
			}
			List<Integer> neighborPlan = Graph.allDijkstra(tp.getThemeParkGraph(), neighborAttOrder, visitor.getPosition(), SystemConst.GRAPH_SIZE);
			double nPlanEval = evalPlan(neighborPlan, tp, visitor);
			
			//WPSCE
			potentialAttOrders.put(nPlanEval, neighborAttOrder);
			
			//優れている方を候補プランとして採用
			if (nPlanEval > cPlanEval) {
				candidatePlan = neighborPlan;
				candidateAttOrder = neighborAttOrder;
				cPlanEval = nPlanEval;
			} else {
				searchCount++;
				if (searchCount == SystemConst.LOCAL_SEARCH_TIMES) {
					break;
				}
			}
		}
		//WPSCE プランの全候補を送信(Mapにしたのは同じプランが含まれないように(keyの重複は許されない)）
		sendPotentialAttOrders(potentialAttOrders);
		
		//statementはプラン受信時にのみ更新（プラン探索時に自身のstatementを除いているので再度送信）
		sendStatement(statement_j);
		
		return null;
	}
	/**
	 * searchPlan()でプラン確定後に呼び出す
	 * ETA[]を更新するためにevalPlan()を呼び出している
	 * @param plan statementを検索するプラン
	 * @param tp テーマパークインスタンス
	 * @param visitor 対象ユーザ
	 * @return statement_jのリスト
	 */
	private ArrayList<Statement> makeStatement(List<Integer> plan, ThemePark tp, Visitor visitor) {
		List <Statement> statement_j = new ArrayList<>();
		evalPlan(plan, tp ,visitor);
		for(int i = 1; i < plan.size(); i++) {
			statement_j.add(new Statement(ownerId, plan.get(i), ETA.get(i)));
		}
		return (ArrayList<Statement>) statement_j;
	}
	
	/**
	 * searchPlan()でmakeStatement()後に呼び出す
	 * @param statement_j 送信するstatement_j
	 */
	private void sendStatement(List<Statement> statement_j) {
		statementServer.receiveStatement(statement_j);
	}
	
	/**
	 * SearchPlan()の最初に呼び出す
	 * 送信していたstatementを取り消す（二重にならないように）
	 * TODO 退場時にも呼び出す必要がある気がするが、後半はアトラクションに乗らないのでおそらく結果変わらない。
	 * TODO statementの処理はplanSearch()から独立さしてもいいかも（ThemePark.simStep()から追加で呼ぶ）
	 */
	private void deleteStatement() {
		statementServer.deleteStatement(statement_j);
	}
	
	/**
	 * テーマパーク全体のstatementをサーバに委譲処理で問い合わせる。
	 * @param attractionId statementを集計する対象のアトラクション
	 * @param currentTime プラン探索時の時刻
	 * @param futureTime 対象アトラクションに到達予定の将来時刻
	 * @return (cTime,fTime]までに到着予定のユーザ数
	 */
	public int getArrivalsUntil(int attractionId, int currentTime, int futureTime) {
		return statementServer.calcStatements(attractionId, currentTime, futureTime);
	}
	
	/**
	 * 全体調整サーバに候補プランの組を全て送信する
	 * @param potentialAttOrders プラン探索で見つかった全候補プランの集合
	 */
	private void sendPotentialAttOrders(Map<Double, List<Integer>> potentialAttOrders) {
		centralServer.receiveAttOrders(this, potentialAttOrders);
	}
	
	/**
	 * 全体調整サーバで定まった最新提示プランを受信して、所持visitorのplanを更新
	 * このときにstatementも更新
	 * 
	 * TODO statementが毎回更新されるので最新プランへの更新が済んでる場合は飛ばすべきかも
	 * @param tp
	 * @param visitor
	 * @return
	 */
	public List<Integer> receivePlan(ThemePark tp, Visitor visitor) {
		List<Integer> plan = new ArrayList<>();
		try {
			List<Integer> adoptedAttOrder = new ArrayList<>(centralServer.getAdoptedAttOrders().get(ownerId));
			//TODO 多分central側でやってるのでよばれることないとおもうけど、調査してから消してください
			//稀に起きないがプラン探索時にはアトラクション訪問前でプラン更新時にアトラクションを終えていると、同じアトラクションをもう一度回ることになりうる
			//その処理visitor側のリストの方が常に小さいため、探索時のadoptedAttOrderより小さいかを確認し、小さいならvisitor側に合わせる
			if (visitor.getAttractionToVisit().size() != adoptedAttOrder.size()) {
				adoptedAttOrder.retainAll(visitor.getAttractionToVisit());
			}
			plan = Graph.allDijkstra(tp.getThemeParkGraph(), adoptedAttOrder, visitor.getPosition(), SystemConst.GRAPH_SIZE);
		}
		catch (NullPointerException | IndexOutOfBoundsException e) {
			plan = visitor.getPlan();
		}
		//TODO 本来はプラン探索時にするべきだと思うが多少の誤差なので実装の手間を考えて許容
		//TODO 毎秒やるならstatement注意
		deleteStatement();
		//statement_jを決定してサーバに送信
		statement_j = makeStatement(plan, tp, visitor);
		sendStatement(statement_j);
		
		return plan;
	}
}
