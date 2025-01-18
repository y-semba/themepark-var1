package setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Graph {
	
	protected List<List<Edge>> adjList = null;
	
	protected Graph(List<Edge> edges, int n){
		adjList = new ArrayList<>();
		
		for (int i = 0; i < n; i++) {
			adjList.add(new ArrayList<>());
		}
		
		for (Edge edge : edges) {
			adjList.get(edge.sorce).add(edge);
		}
	}
	
	/**
	 * 内部でdijkstra()を呼び、planの順番に入口->targetAttList[i]->出口までの最短経路を探索
	 * @param graph 探索グラフ(これは同じ設定では固定、定数的に与えるがThemeParkのフィールドとしている)
	 * @param targetAttList 訪問予定のアトラクションリスト
	 * @param position 探索時点での現在地
	 * @param n ノードの数(dijkstraへ引数で指定するため) GRAPH_SIZEで固定です。
	 * @return 入口->targetAttList[i]->出口までの最短経路List<Integer>
	 */
	public static List<Integer> allDijkstra(Graph graph, List<Integer> targetAttList, int position, int n) {
		List<Integer> route = new ArrayList<>();
		//アトラクションを回り終えている場合は現在地から出口へ
		if (targetAttList.size() == 0) {
			route = dijkstra(graph, position, SystemConst.EXIT, n);
			return route;
		}
		//現在地->attraction[0]
		route.addAll(dijkstra(graph, position, targetAttList.get(0), n));
		//attraction[i]->attraction[i+1]　残り１つの場合は実行しない(上でやってる)
		for (int i = 0; i < targetAttList.size() - 1; i++) {
			//[0]->[1], [1]->[2]のように終点と始点が重複するので経路接続前に終点を消す
			int overlapIndex = route.size() - 1;
			route.remove(overlapIndex);
			int source = targetAttList.get(i);
			int dest = targetAttList.get(i+1);
			route.addAll(dijkstra(graph, source, dest, n));
		}
		//attraction[lastIndex] -> EXIT
		int planLastIndex = targetAttList.size() - 1;
		int overlapIndex = route.size() - 1;
		route.remove(overlapIndex);
		route.addAll(dijkstra(graph, targetAttList.get(planLastIndex),SystemConst.EXIT, n));
		return route;
	}
	/**
	 * prev[i] < 0 となるprev[source]まで実行する再帰関数
	 * 呼び出し元のListに経由地をadd()する。dijkstra()で呼び出される
	 * 
	 * @param prev 最小距離を更新した際の経由地の配列
	 * @param i 注目しているノード, 再帰関数でprev[i]とすることで経由地を
	 * @param route 経由地リスト
	 */
	private static void getRoute(int[] prev, int i, List<Integer> route) {
		if (i >= 0) {
			getRoute(prev, prev[i], route);
			route.add(i);
		}
	}
	
	/**
	 * @param graph 探索するグラフ構造
	 * @param source 経路探索の出発地点
	 * @param n グラフの頂点の個数
	 */
	public static List<Integer> dijkstra(Graph graph, int source, int dest, int n) {
		PriorityQueue<Node> minHeap;
		//TotalFunctionインタフェースのapplyAsIntオーバーライドのラムダ形式でTotalFunctionをインスタンス化してるはず(20221227変更履歴を参照)
		minHeap = new PriorityQueue<>(Comparator.comparingInt(node -> node.weight));	
		minHeap.add(new Node(source, 0));
		
		//ソースから'v'までの初期距離は無限大として設定
		List<Integer> dist;
		dist = new ArrayList<>(Collections.nCopies(n, Integer.MAX_VALUE));
		dist.set(source, 0);
		
		//最小距離が確定したかどうか
		boolean[] done = new boolean[n];
		done[source] = true;
		
		//最小距離が確定した際の経由地を記録
		int[] prev = new int[n];
		prev[source] = -1;
		
		while (!minHeap.isEmpty()) {
			//最小距離確定したノード
			Node node = minHeap.poll();
			int u = node.vertex;
			
			//探索目的の最短経路が確定した時点で終了
			if(u == dest) break;
			
			//最小距離が確定したノード'u'の隣接ノード'v'に対して実行
			for (Edge edge : graph.adjList.get(u)) {
				int v = edge.dest;
				int weight = edge.weight;
				
				//'u'を経由した際の'v'までの距離を計算、dist[v]と比較して最短経路を更新
				if(!done[v] && dist.get(u) + weight < dist.get(v)) {
					dist.set(v, dist.get(u) + weight);
					prev[v] = u;
					minHeap.add(new Node(v, dist.get(v)));
				}
			}
			done[u] = true;
		}
		
		List<Integer> route = new ArrayList<>();
		
		//'dest'までの経路をprev[]を使用して求める
		if (dest != source && dist.get(dest) != Integer.MAX_VALUE) {
			getRoute(prev, dest, route);
			//System.out.printf("Path (%d -> %d): Minimum cost = %d, Route = %s\n", source, dest, dist.get(dest), route);
		}
		return route;
	}
}

class Edge {
	int sorce, dest, weight;
	
	public Edge(int sorce, int dest, int weight) {
		this.sorce = sorce;
		this.dest = dest;
		this.weight = weight;
	}
}


class Node {
	int vertex, weight;
	
	/**
	 * 
	 * @param vertex 頂点の通し番号
	 * @param weight 確定した最短コスト
	 */
	public Node(int vertex, int weight) {
		this.vertex = vertex;
		this.weight = weight;
	}
}