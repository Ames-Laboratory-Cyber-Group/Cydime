package gov.ameslab.cydime.preprocess;

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

public class MakeDayGraph {
	
	private static final String ALL_SERVICES = "all";
	
	private static boolean mDoNormalize;
	private static Map<String, String> mIPNetname;
	private static Set<String> mActiveInts;
	private static Set<String> mActiveExts;
	
	public static void main(String[] args) throws IOException {
		if (args.length == 1 && args[0].equalsIgnoreCase("-norm")) {
			mDoNormalize = true;
		}
		
		mIPNetname = Preprocess.readWhois();
		mActiveInts = CUtil.makeSet(FileUtil.readFile(Config.ACTIVE_INTERNAL_FILE, true));
		mActiveExts = CUtil.makeSet(FileUtil.readFile(Config.ACTIVE_EXTERNAL_FILE, false));
		
		Map<String, Map<String, Map<String, Set<String>>>> servIntNetnameDays = readDays(args);
		for (Entry<String, Map<String, Map<String, Set<String>>>> entry : servIntNetnameDays.entrySet()) {
			String serv = entry.getKey();
			Map<String, Map<String, Double>> g = toWeighted(entry.getValue());
			if (mDoNormalize) {
				System.out.println("Normalizing output...");
				g = normalize(g);
			}
			Preprocess.writeGraph("days_" + serv, g);
		}
	}
	
	private static Map<String, Map<String, Map<String, Set<String>>>> readDays(String[] args) throws IOException {
		Map<String, Map<String, Map<String, Set<String>>>> servIntNetnameDays = CUtil.makeMap();
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
				for (String serv : ServiceParser.parse(src, dest)) {
					incrementService(servIntNetnameDays, intIP, netname, day, serv);
				}
				incrementService(servIntNetnameDays, intIP, netname, day, ALL_SERVICES);
			}
			in.close();
		}
		
		return servIntNetnameDays;
	}

	private static void incrementService(Map<String, Map<String, Map<String, Set<String>>>> servIntNetnameDays, String intIP, String netname, String day, String serv) {
		Map<String, Map<String, Set<String>>> intNetnameDays = servIntNetnameDays.get(serv);
		if (intNetnameDays == null) {
			intNetnameDays = CUtil.makeMap();
			servIntNetnameDays.put(serv, intNetnameDays);
		}
		
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

	private static Map<String, Map<String, Double>> toWeighted(Map<String, Map<String, Set<String>>> intNetnameDays) {
		Map<String, Map<String, Double>> intNetnameWeight = CUtil.makeMap();
		for (Entry<String, Map<String, Set<String>>> entry : intNetnameDays.entrySet()) {
			String intIP = entry.getKey();
			Map<String, Double> netnameWeight = CUtil.makeMap();
			for (Entry<String, Set<String>> dayEntry : entry.getValue().entrySet()) {
				String netname = dayEntry.getKey();
				netnameWeight.put(netname, Double.valueOf(dayEntry.getValue().size()));
			}
			
			intNetnameWeight.put(intIP, netnameWeight);
		}
		
		return intNetnameWeight;
	}

	private static Map<String, Map<String, Double>> normalize(Map<String, Map<String, Double>> intNetnameWeight) {
		double maxDays = 0.0;
		for (Entry<String, Map<String, Double>> entry : intNetnameWeight.entrySet()) {
			for (double w : entry.getValue().values()) {
				if (w > maxDays) {
					maxDays = w;
				}
			}
		}
		
		Map<String, Map<String, Double>> norm = CUtil.makeMap();
		for (Entry<String, Map<String, Double>> entry : intNetnameWeight.entrySet()) {
			String intIP = entry.getKey();
			Map<String, Double> netnameWeight = CUtil.makeMap();
			
			for (Entry<String, Double> dayEntry : entry.getValue().entrySet()) {
				String netname = dayEntry.getKey();
				double weight = dayEntry.getValue() / maxDays;
				netnameWeight.put(netname, weight);
			}
			
			norm.put(intIP, netnameWeight);
		}
		return norm;
	}

}
