package environment;

import java.util.List;

import setting.SystemConst;

class StatementServer {
	//Singletonパターン
	private static StatementServer statementServer = new StatementServer(SystemConst.GRAPH_SIZE, SystemConst.MAX_TIME);
	
	private int[][] statements = null;
	
	/**
	 * statements[attractionId][time]の2次元リストを作成
	 * @param nodeId ノード番号
	 * @param time 現在時刻〜将来時刻
	 */
	private StatementServer(int nodeId, int time) {
		statements = new int[nodeId][time];
	}
	public static StatementServer getInstance() {
        return statementServer;
    }
	/**
	 * Deviceから委譲されるメソッド
	 * visitor_jのステートメントをサーバ側で集約する
	 * @param statement_j visitor_jのstatementリスト
	 */
	public void receiveStatement(List<Statement> statement_j) {
		for (Statement statement : statement_j) {
			statements[statement.attractionId][statement.eta]++;
		}
	}
	/**
	 * Deviceから委譲されるメソッド
	 * visitor_j(j=id)のstatementをサーバから削除する
	 * @param visitorId 対象visitor_j
	 */
	public void deleteStatement(List<Statement> statement_j) {
		for (Statement statement : statement_j) {
			statements[statement.attractionId][statement.eta]--;
		}
	}
	/**
	 * Deviceから委譲されるメソッド
	 * Device.getArrivalsUntil();
	 * 
	 * @param attractionId statementを集計する対象のアトラクション
	 * @param currentTime プラン探索時の時刻
	 * @param futureTime 対象アトラクションに到達予定の将来時刻
	 * @return (cTime,fTime]までに到着予定のユーザ数
	 */
	public int calcStatements(int attractionId, int currentTime, int futureTime) {
		int expectedArrivals = 0;
		for (int time = currentTime; time <= futureTime; time++) {
			expectedArrivals += statements[attractionId][time];
		}
		return expectedArrivals;
	}
}

class Statement { 
	int visitorId;
	int attractionId;
	int eta;
	
	public Statement(int visitorId, int attractionId, int eta) {
		this.visitorId = visitorId;
		this.attractionId = attractionId;
		this.eta = eta;
	}
}