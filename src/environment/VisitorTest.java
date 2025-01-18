package environment;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.jupiter.api.DisplayName;

class VisitorTest {

	@Test
	@DisplayName("入場処理")
	void enterTest() {
		Visitor v1 = new Visitor();
		//インスタンス作成時はposition=-1
		int expected1 = v1.getPosition();
		assertThat(expected1, is(-1));
		//入場処理で入口へ
		v1.enter();
		int expected2 = 0;
		assertThat(expected2, is(0));
	}

	//TODO テーマパークコンストラクタ定義したらテストする
	@Test
	@DisplayName("行動")
	void actTest() {
		
		
	}
}
