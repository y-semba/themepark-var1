package environment;

import java.util.ArrayList;
import java.util.List;

import setting.EnumStatus;
import setting.Graph;
import setting.SystemConst;

public class SCEDevice extends Device {
	private StatementServer statementServer;
	List<Statement> statement_j = new ArrayList<>();
	public SCEDevice(int userId) {
		super(userId);
		statementServer = StatementServer.getInstance();
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
		ETA.add(tp.getSimTime());
		for (int i = 1; i < plan.size(); i++) {
			ETA.add(ETA.get(i - 1) + costEstimate(plan.get(i-1), ETA.get(i - 1), tp, visitor));
		}
		return -ETA.get(plan.size() - 1);
	}

	@Override
	public List<Integer> searchPlan(ThemePark tp, Visitor visitor) {
		//前回のstatementを削除
		deleteStatement();
		
		//候補プランとして初期プランの決定と評価値を計算
		List<Integer> candidateAttOrder = new ArrayList<>(visitor.getAttractionToVisit());
		List<Integer> candidatePlan = Graph.allDijkstra(tp.getThemeParkGraph(), candidateAttOrder, visitor.getPosition(), SystemConst.GRAPH_SIZE);
		double cPlanEval = evalPlan(candidatePlan, tp, visitor);
		
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
		//statement_jを決定してサーバに送信
		statement_j = makeStatement(candidatePlan, tp, visitor);
		sendStatement(statement_j);
		return candidatePlan;
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
}