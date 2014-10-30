package gov.ameslab.cydime.prefilter;

import gov.ameslab.cydime.preprocess.Preprocess;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PreFilter {
	
	private static final double THRESHOLD_LEAF = 0.1;
	private static final double THRESHOLD_MAJORITY = 1.0 / 3.0;
	
	private static Map<String, String> mIPNetname;
	private static Set<String> mActiveInts;
	private static Set<String> mActiveExts;
	private static Map<String, String> mIntSubnet;
	private static Map<String, String> mIntAffl;
	private static OrgTree mTree;
	
	public static void main(String[] args) throws IOException {
		mIPNetname = Preprocess.readWhois();
		mActiveInts = CUtil.makeSet(FileUtil.readFile(Config.ACTIVE_INTERNAL_FILE, true));
		mActiveExts = CUtil.makeSet(FileUtil.readFile(Config.ACTIVE_EXTERNAL_FILE, false));
		mIntSubnet = FileUtil.readCSV(Config.IP_NETREG_FILE, 0, 1, true, true);
		mIntAffl = FileUtil.readCSV(Config.IP_NETREG_FILE, 0, 2, true, true);
		
		Map<String, Map<String, Set<String>>> intNetnameDays = readDays(args);
		Map<String, Map<String, Double>> intNetnameDaysNorm = normalize(intNetnameDays);
//		System.out.println(intNetnameDaysNorm);
		Map<String, Set<String>> intNetnameLeaf = makeLeaves(intNetnameDaysNorm);
//		System.out.println(intNetnameLeaf);
		mTree = makeTree(intNetnameLeaf);
		populateLeaves(mTree, intNetnameLeaf);
		mTree.propagateUp(THRESHOLD_MAJORITY);
		mTree.propagateDown();
		mTree.extractNodeFilteredData();
//		mTree.printMajority(System.out);
		mTree.printFilter(System.out);
//		mTree.printNode(System.out);
	}
	
//	private static OrgTree makeTree(Map<String, String> intNetreg, Map<String, Set<String>> intNetnameLeaf) {
//		OrgTree tree = new OrgTree();
//		for (Entry<String, String> entry : intNetreg.entrySet()) {
//			String intIP = entry.getKey();
//			String netreg = entry.getValue();
//			if (!intNetnameLeaf.containsKey(intIP)) continue;
//			
//			tree.addChild(OrgTree.ROOT, netreg);
//			tree.addChild(netreg, intIP);
//		}		
//		return tree;
//	}
	
	private static OrgTree makeTree(Map<String, Set<String>> intNetnameLeaf) {
		OrgTree tree = new OrgTree();
		for (Entry<String, String> entry : mIntSubnet.entrySet()) {
			String intIP = entry.getKey();
			String subnet = entry.getValue();
			String affl = mIntAffl.get(intIP);
			if (!intNetnameLeaf.containsKey(intIP)) continue;
			
			String cross = subnet + " * " + affl;
			tree.addChild(OrgTree.ROOT, affl);
			tree.addChild(affl, cross);
			tree.addChild(cross, intIP);
		}		
		return tree;
	}

	private static void populateLeaves(OrgTree tree, Map<String, Set<String>> intNetnameLeaf) {
		for (Entry<String, Set<String>> entry : intNetnameLeaf.entrySet()) {
			OrgTreeNode node = tree.getNode(entry.getKey());
			if (node == null) continue;
			
			node.setData(entry.getValue());
		}
	}

	private static Map<String, Map<String, Set<String>>> readDays(String[] args) throws IOException {
		Map<String, Map<String, Set<String>>> intNetnameDays = CUtil.makeMap();
		for (String file : args) {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				if (!mActiveInts.contains(split[0]) || !mActiveExts.contains(split[1])) continue;

				String intIP = split[0];
				String netname = mIPNetname.get(split[1]);
				if (netname == null) continue;
				
				String src = split[2];
				String dest = split[3];
				String day = split[4];
				incrementService(intNetnameDays, intIP, netname, day);
			}
			in.close();
		}
		
		return intNetnameDays;
	}

	private static void incrementService(Map<String, Map<String, Set<String>>> intNetnameDays, String intIP, String netname, String day) {
		Map<String, Set<String>> netnameDays = intNetnameDays.get(intIP);
		if (netnameDays == null) {
			netnameDays = CUtil.makeMap();
			intNetnameDays.put(intIP, netnameDays);
		}
		
		Set<String> days = netnameDays.get(netname);
		if (days == null) {
			days = CUtil.makeSet();
			netnameDays.put(netname, days);
		}
		
		days.add(day);
	}
	
	private static Map<String, Map<String, Double>> normalize(Map<String, Map<String, Set<String>>> intNetnameDays) {
		int maxDays = 0;
		for (Entry<String, Map<String, Set<String>>> entry : intNetnameDays.entrySet()) {
			for (Set<String> daySet : entry.getValue().values()) {
				int days = daySet.size();
				if (days > maxDays) {
					maxDays = days;
				}
			}
		}
		
		Map<String, Map<String, Double>> norm = CUtil.makeMap();
		for (Entry<String, Map<String, Set<String>>> entry : intNetnameDays.entrySet()) {
			String intIP = entry.getKey();
			Map<String, Double> netnameWeight = CUtil.makeMap();
			
			for (Entry<String, Set<String>> dayEntry : entry.getValue().entrySet()) {
				String netname = dayEntry.getKey();
				double weight = (double) dayEntry.getValue().size() / maxDays;
				netnameWeight.put(netname, weight);
			}
			
			norm.put(intIP, netnameWeight);
		}
		return norm;
	}

	private static Map<String, Set<String>> makeLeaves(Map<String, Map<String, Double>> intNetnameDaysNorm) {
		Map<String, Set<String>> intLeaves = CUtil.makeMap();
		for (Entry<String, Map<String, Double>> entry : intNetnameDaysNorm.entrySet()) {
			Set<String> leaves = CUtil.makeSet();
			for (Entry<String, Double> netEntry : entry.getValue().entrySet()) {
				if (netEntry.getValue() > THRESHOLD_LEAF) {
					leaves.add(netEntry.getKey());
				}
			}
			intLeaves.put(entry.getKey(), leaves);
		}
		return intLeaves;
	}

}
