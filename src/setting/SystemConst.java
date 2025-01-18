package setting;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class SystemConst {
	/** インスタンスの生成禁止 */
	private SystemConst() {
	}
	/** 0: 入口, 1~10: アトラクション, 11~19: 道路, 20: 出口 */
	public static final int GRAPH_SIZE = 21;
	public static final int ENTRANCE = 0;
	public static final int EXIT = 20;
	public static final int NUM_OF_ATTRACTION = 10;

	/** シミュレーションの設定 */
	public static final int MAX_USER = 3000;	// パラメータ
	public static final int MAX_TIME = 35000;	// パラメータ(MAX_USERの12倍くらいにしておく．大きく設定するとメモリの無駄遣いするだけで動作に影響はない)
	public static final int SIM_SEED = 9;		// パラメータ
	public static final EnumMethod METHOD = EnumMethod.WPSCE;	// パラメータ(CCE/SCE/WSCE/WPSCE)

	/** 選好値設定 */
	// "WT", "MT", "TT", "smooth", "bias"のように使うutilityをこの順番で指定する
	// Set.ofとしないのは、順序を維持するため
	public static Set<String> PREFERENCES = new LinkedHashSet<>(Arrays.asList("WT","MT"));	// パラメータ
	public static final long PREF_SEED = SIM_SEED;
	public static final Random PREF_RND = new Random(PREF_SEED);

	/** WPSCEのシミュレーション探索設定 */
	public static final int SIM_SEARCH_TIMES = 30;	// パラメータ(T)
	public static final double SIM_SEARCH_PROB = 0.001;	// パラメータ(cp)
	public static final double CANDIDATE_PERCENTAGE = 50;	// パラメータ.最適プランの上位A%を候補として残す(0%で最適に一致)
	public static final int SIM_PLANNING_INTERVAL = 100;	// パラメータ(interval)
	public static final long CLONED_ENT_SEED = SIM_SEED;	// simSearchで入場する際に他の設定と乱数を合わせるため
	public static final Random CLONED_ENT_RND = new Random(CLONED_ENT_SEED);
	public static final long SIM_SEARCH_SEED = SIM_SEED;
	public static final Random SIM_SEARCH_RND = new Random(SIM_SEARCH_SEED);

	/** SAの探索設定 */
	public static final double TEMPERATURE = 0; //パラメータ(0で山登り法に一致）
	public static final double COOL = 0.995; //冷却率（SA評価式等で調べて）
	public static final long SA_SEED = SIM_SEED;
	public static final Random SA_RND = new Random(SA_SEED);
	
	/** 訪問アトラクション設定 */
	public static final long DECIDE_ATT_SEED = SIM_SEED;
	public static final int NUM_ATT_TO_VISIT = 4;
	public static final Random DECIDE_ATT_RND = new Random(DECIDE_ATT_SEED);

	/** ポアソン分布に従った入場で使う乱数とパラメータ */
	public static final double POISSON_RMD = 0.0001 * MAX_USER;
	public static final long ENT_SEED = SIM_SEED;
	public static final Random ENT_RND = new Random(ENT_SEED);
	public static final Random TP_RND = new Random(ENT_SEED);
	public static final double[] POISSON_DIS = SystemCalc.poissonDis();

	/** 山登り法の回数とプランニングインターバル */
	public static final int VISITOR_PLANNING_INTERVAL = 100;	// パラメータ(interval_j)
	public static final int LOCAL_SEARCH_TIMES = 15; //lambda

	/** resultファイルの保存パス */
	public static String FILE_PATH = "./results/" + METHOD + "/" + MAX_USER + "/";
}