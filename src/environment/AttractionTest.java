package environment;

//import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttractionTest {

	@Test
	@DisplayName("行列に並ぶ処理()とサービスを受ける判定の処理")
	void twentyVisitor() {
		boolean[] expected = new boolean[20];
		for(int i = 0; i < 20; i++) {
			if(i < 14) expected[i] = true;
			if(i >= 14) expected[i] = false;
		}
		boolean[] result = new boolean[20];
		Visitor[] vis = new Visitor[20];
		Attraction at = new Attraction(8, 310);
		//待ち行列0なのでoperation++;
		//あと14枠空いてる
		assertThat(at.hasEmpty(),is(true));
		//行列は[0]~[19]まで並ぶ
		for(int i = 0; i < 20; i++) {
			vis[i] = new Visitor();
			at.registerQueue(vis[i].getId());
		}
		//行列埋まってるのでfalse
		assertThat(at.hasEmpty(),is(false));
		//サービス受けれるかチェック
		for(int i = 0; i < 20; i++) {
			result[i] = at.canServe(vis[i].getId());
		}
		//14枠が空いてるので[0]~[13]までtrue
		assertThat(result,is(expected));
	}

	
}
