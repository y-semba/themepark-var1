package environment;

import java.util.ArrayList;
import java.util.List;

import setting.EnumStatus;
import setting.Graph;
import setting.SystemConst;

public class CCEDevice extends Device {

	public CCEDevice(int userId) {
		super(userId);
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
			int queue = ((Attraction)currentNode).getQueueLength();
			int window = ((Attraction)currentNode).getCapacity();
			int serviceTime = ((Attraction)currentNode).getServiceTime();
			return (queue / window + 1) * serviceTime;
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
		return candidatePlan;
	}
}