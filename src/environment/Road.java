package environment;

public class Road extends ThemeParkNode {

	public Road(int nodeId, int serviceTime, int capacity) {
		super(nodeId, serviceTime, capacity);
	}
	public Road(int nodeId) {
		super(nodeId, 200, Integer.MAX_VALUE);
	}

	@Override
	public boolean hasEmpty() {
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
	public Road clone() {
		Road road = null;
		try {
			road = (Road)super.clone();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return road;
	}
}