package environment;

import java.util.List;

import setting.NodeFactory;
import setting.ThemeParkGraph;
import setting.VisitorFactory;

public class Main {
	public static void main(String[] args) {
		//テーマパーク要素生成
		ThemeParkGraph tpg = new ThemeParkGraph();
		NodeFactory nodeFactory = new NodeFactory();
		List<ThemeParkNode> nodes = nodeFactory.initNode();
		VisitorFactory visitorFactory = new VisitorFactory();
		List<Visitor> visitors = visitorFactory.initVisitor();
		//テーマパーク生成
		ThemePark tp = new ThemePark(tpg, nodes, visitors);
		//Observersの作成・追加
		Observer visitorsObserver = new VisitorsObserver();
		Observer nodesObserver = new NodesObserver();
		Observer plansSetObserver = new PlansSetObserver();
		tp.calcEntryTime(visitors);
		tp.addObserver(visitorsObserver);
		tp.addObserver(nodesObserver);
		tp.addObserver(plansSetObserver);
		
		//ユーザ行動
		System.out.println("simulation start!");
		tp.sim();
	}
}
