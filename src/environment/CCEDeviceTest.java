package environment;

//import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import setting.NodeFactory;
import setting.ThemeParkGraph;

class CCEDeviceTest {

	@DisplayName("CCEDeviceのテスト")
	@Test
	void costEstimateTest() {
		CCEDevice cce = new CCEDevice(1);
		ThemeParkGraph tpg = new ThemeParkGraph();
		List<Visitor> visitors = new ArrayList<Visitor>();
		Visitor v1 = new Visitor();
		visitors.add(v1);
		NodeFactory nf = new NodeFactory();
		List<ThemeParkNode> nodes = nf.initNode();
		ThemePark tp = new ThemePark(tpg, nodes, visitors);
		// v1.enter();
		// tが現在時刻の時の入口での利用
		int result = cce.costEstimate(0, 0, tp, v1);
		assertThat(result, is(0));
		// tが現在時刻の時の道路ノード（初期プラン時に起こる)
		result = cce.costEstimate(11, 0, tp, v1);
		assertThat(result, is(200));
		// tが未来時刻の道路ノード
		result = cce.costEstimate(15, 5, tp, v1);
		assertThat(result, is(200));
		// tが未来時刻の時のアトラクション（待ち行列なし）
		// (queue(0) / window(15) + 1) * serviceTime(290);
		result = cce.costEstimate(9, 200, tp, v1);
		assertThat(result, is(290));
		v1.initVisitor();
		v1.planSearch(tp);
		v1.act(tp);
		// node[0]->[11]
		// 移動直後のramainingTime=200
		result = cce.costEstimate(11, 0, tp, v1);
		assertThat(result, is(200));
		// 移動後もう一度行動しているのでremainigTime=199
		v1.act(tp);
		result = cce.costEstimate(11, 0, tp, v1);
		assertThat(result, is(199));
	}

}
