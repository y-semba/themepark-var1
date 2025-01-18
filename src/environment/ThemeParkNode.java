package environment;

public abstract class ThemeParkNode implements Cloneable {
	protected int nodeId;
	protected int serviceTime;
	protected int capacity;	
	
	public ThemeParkNode(int nodeId, int serviceTime, int capacity) {
		this.nodeId = nodeId;
		this.serviceTime = serviceTime;
		this.capacity = capacity;
	}
	
	public int getNodeId() {
		return nodeId;
	}
	public int getServiceTime() {
		return serviceTime;
	}
	public int getCapacity() {
		return capacity;
	}
	
	public abstract boolean hasEmpty();
	
	public abstract boolean canServe(int visitorId);
	
	public abstract void finishService();
	
	@Override
	public ThemeParkNode clone() {
		ThemeParkNode themeParkNode = null;
		try {
			themeParkNode = (ThemeParkNode)super.clone();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return themeParkNode;
	}
}
