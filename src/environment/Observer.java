package environment;

public interface Observer {
	/**
	 * notifyObserver()毎に呼び出される関数
	 * @param tp 監視対象のテーマパークインスタンス
	 */
	public abstract void update(ThemePark tp);
	/**
	 * endObserver()で最後に呼び出される処理
	 * File.close()のために実装
	 * @param tp 監視対象のテーマパークインスタンス
	 */
	public abstract void end(ThemePark tp);
}
