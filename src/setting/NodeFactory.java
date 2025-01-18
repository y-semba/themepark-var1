package setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import environment.Attraction;
import environment.Entrance;
import environment.Exit;
import environment.Road;
import environment.ThemeParkNode;

public class NodeFactory {
	public ArrayList<ThemeParkNode> initNode() {
		List<ThemeParkNode> nodes = Arrays.asList(
			new Entrance(),
			new Attraction(1, 310),
			new Attraction(2, 280),
			new Attraction(3, 300),
			new Attraction(4, 320),
			new Attraction(5, 280),
			new Attraction(6, 300),
			new Attraction(7, 290),
			new Attraction(8, 310),
			new Attraction(9, 290),
			new Attraction(10, 320),
			new Road(11),
			new Road(12),
			new Road(13),
			new Road(14),
			new Road(15),
			new Road(16),
			new Road(17),
			new Road(18),
			new Road(19),
			new Exit()
		);
		return new ArrayList<ThemeParkNode>(nodes);
	}
	
}
