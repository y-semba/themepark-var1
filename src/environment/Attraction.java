package environment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Attraction extends ThemeParkNode {
	private List<Integer> waitingQueue = new LinkedList<>();
	private int operation;

	public Attraction(int nodeId, int serviceTime, int capacity) {
		super(nodeId, serviceTime, capacity);
	}
	public Attraction(int nodeId, int serviceTime) {
		super(nodeId, serviceTime, 15);
	}

	@Override
	public boolean hasEmpty() {
		if (operation < capacity && waitingQueue.isEmpty()) {
			operation++;
			return true;
		}
		return false;
	}

	@Override
	public boolean canServe(int visitorId) {
		//現在空いているサービスウィンド
		int end = capacity - operation;
		if (operation >= capacity) return false;
		//待ち行列の先頭から空き人数分までのサブリストに含まれているか判定
		List<Integer> visitors = new ArrayList<>(waitingQueue);
		if (visitors.size() < end) end = visitors.size();
		if (visitors.subList(0, end).contains(visitorId)) {
			waitingQueue.remove((Integer)visitorId);
			operation++;
			return true;
		}
		return false;
	}
	
	public void registerQueue(int visitorId) {
		waitingQueue.add(visitorId);
	}
	
	@Override
	public void finishService() {
		operation--;
	}

	public int getQueueLength() {
		return waitingQueue.size();
	}
	public int getPriorQueueLength(int visitorId) {
		return waitingQueue.indexOf(visitorId);
	}
	
	@Override
	public Attraction clone() {
		Attraction attraction = null;
		try {
			attraction = (Attraction)super.clone();
			attraction.waitingQueue = new LinkedList<>(this.waitingQueue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return attraction;
	}
}
