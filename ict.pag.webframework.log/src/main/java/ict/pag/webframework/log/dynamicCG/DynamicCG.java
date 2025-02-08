package ict.pag.webframework.log.dynamicCG;

import java.util.ArrayList;

public class DynamicCG {

	private ArrayList<DynamicCGNode> roots = new ArrayList<>();
	private ArrayList<DynamicCGNode> nodes = new ArrayList<>();

	private ArrayList<DynamicCGNode> nodes_unknowFather = new ArrayList<>();

	public ArrayList<DynamicCGNode> getRoots() {
		return roots;
	}

	public ArrayList<DynamicCGNode> getNodes() {
		return nodes;
	}

	public ArrayList<DynamicCGNode> getNodes_unknowFather() {
		return nodes_unknowFather;
	}

}
