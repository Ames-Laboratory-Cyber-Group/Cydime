package gov.ameslab.cydime.prefilter;

import gov.ameslab.cydime.util.CUtil;

import java.io.PrintStream;
import java.util.Map;

public class OrgTree {

	public static final String ROOT = "root";
	
	private Map<String, OrgTreeNode> mNodeMap;
	
	public OrgTree() {
		mNodeMap = CUtil.makeMap();
		mNodeMap.put(ROOT, new OrgTreeNode(ROOT));
	}
	
	public void addChild(String parent, String child) {
		if (getNode(child) != null) return;
		
		OrgTreeNode pNode = getNode(parent);
		if (pNode == null) throw new IllegalArgumentException("Parent node " + parent + " not exists.");
		
		OrgTreeNode cNode = new OrgTreeNode(child);
		mNodeMap.put(child, cNode);
		pNode.addChild(cNode);
	}
	
	public OrgTreeNode getNode(String name) {
		return mNodeMap.get(name);
	}

	public void propagateUp(double thresholdMajority) {
		getNode(ROOT).propagateUp(thresholdMajority);
	}

	public void propagateDown() {
		getNode(ROOT).propagateDown();
	}

	public void extractNodeFilteredData() {
		getNode(ROOT).extractNodeFilteredData();
	}

	public void printMajority(PrintStream out) {
		getNode(ROOT).printMajority(out);
	}

	public void printFilter(PrintStream out) {
		getNode(ROOT).printFilter(out);
	}

	public void printNode(PrintStream out) {
		getNode(ROOT).printNode(out);
	}

}
