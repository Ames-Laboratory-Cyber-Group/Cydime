package gov.ameslab.cydime.prefilter;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Histogram;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class OrgTreeNode {

	private String mName;
	private OrgTreeNode mParent;
	private List<OrgTreeNode> mChildren;
	private Set<String> mMajority;
	private Set<String> mFilter;
	
	public OrgTreeNode(String name) {
		mName = name;
		mChildren = CUtil.makeList();
		mMajority = CUtil.makeSet();
		mFilter = CUtil.makeSet();
	}

	public void addChild(OrgTreeNode child) {
		mChildren.add(child);
		child.mParent = this;
	}

	public void setData(Set<String> data) {
		mMajority = data;
	}

	public void printMajority(PrintStream out) {
		if (!mFilter.isEmpty()) {
			if (mParent == null) {
				out.println("[Node: " + mName + "]");
			} else {
				out.println("[Node: " + mName + " Parent: " + mParent.mName + "]");
			}
			List<String> sorted = CUtil.makeList(mMajority);
			Collections.sort(sorted);
			for (String f : sorted) {
				out.println(f);
			}
		}
		
		for (OrgTreeNode c : mChildren) {
			c.printMajority(out);
		}
	}
	
	public void printFilter(PrintStream out) {
		if (mChildren.isEmpty()) return;
		
		if (!mFilter.isEmpty()) {
			if (mParent == null) {
				out.println("[Node: " + mName + "]");
			} else {
				out.println("[Node: " + mName + " Parent: " + mParent.mName + "]");
			}
			List<String> sorted = CUtil.makeList(mFilter);
			Collections.sort(sorted);
			for (String f : sorted) {
				out.println(f);
			}
			
			out.println();
		}
		
		for (OrgTreeNode c : mChildren) {
			c.printFilter(out);
		}
	}

	public void printNode(PrintStream out) {
		if (mParent == null) {
			out.println("[Node: " + mName + "]");
		} else {
			out.println("[Node: " + mName + " Parent: " + mParent.mName + "]");
		}
		
		for (OrgTreeNode c : mChildren) {
			c.printNode(out);
		}
	}

	public void propagateUp(double thresholdMajority) {
		if (mChildren.isEmpty()) return;
			
		for (OrgTreeNode c : mChildren) {
			c.propagateUp(thresholdMajority);
		}
		
		Histogram<String> count = new Histogram<String>();
		for (OrgTreeNode c : mChildren) {
			for (String m : c.mMajority) {
				count.increment(m);
			}
		}
		
		double threshold = mChildren.size() * thresholdMajority;
		for (Entry<String, Double> entry : count.entrySet()) {
			if (entry.getValue() >= threshold) {
				mMajority.add(entry.getKey());
			}
		}
		
//		List<String> sortedKeysByValue = count.getSortedKeysByValue();
//		Collections.reverse(sortedKeysByValue);
//		
//		for (int i = 0; i < sortedKeysByValue.size(); i++) {
//			String key = sortedKeysByValue.get(i);
//			if (count.get(key) <= threshold && i >= 5) break;
//			
//			mMajority.add(key);
//		}
	}

	public void propagateDown() {
		for (OrgTreeNode c : mChildren) {
			c.mMajority.addAll(mMajority);
			c.propagateDown();
		}
	}

	public void extractNodeFilteredData() {
		mFilter = CUtil.makeSet();
		mFilter.addAll(mMajority);
		
		if (mParent != null) {
			mFilter.removeAll(mParent.mMajority);
		}
		
		for (OrgTreeNode c : mChildren) {
			c.extractNodeFilteredData();
		}
	}

}
