package environment;

public class Entrance extends ThemeParkNode {

	public Entrance(int nodeId, int serviceTime, int capacity) {
		super(nodeId, serviceTime, capacity);
	}
	public Entrance() {
		super(0, 0, Integer.MAX_VALUE);
	}

	@Override
	public boolean hasEmpty() {
		System.exit(1);
		return true;
	}
	@Override
	public boolean canServe(int visitorId) {
		return true;
	}
	@Override
	public void finishService() {
		//何もしない
	}
	@Override
	public Entrance clone() {
		Entrance entrance = null;
		try {
			entrance = (Entrance)super.clone();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entrance;
	}
}