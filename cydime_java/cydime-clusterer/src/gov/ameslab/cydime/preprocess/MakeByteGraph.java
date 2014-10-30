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

public class MakeByteGraph {
	
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
		
		Map<String, Map<String, Map<String, Double>>> servIntNetnameBytes = readBytes(args);
		for (Entry<String, Map<String, Map<String, Double>>> entry : servIntNetnameBytes.entrySet()) {
			String serv = entry.getKey();
			Map<String, Map<String, Double>> g = entry.getValue();
			if (mDoNormalize) {
				System.out.println("Normalizing output...");
				g = normalize(g);
			}
			Preprocess.writeGraph("byte_" + serv, g);
		}
	}
	
	private static Map<String, Map<String, Map<String, Double>>> readBytes(String[] args) throws IOException {
		Map<String, Map<String, Map<String, Double>>> servIntNetnameBytes = CUtil.makeMap();
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
				double bytes = Double.parseDouble(split[7]);
				for (String serv : ServiceParser.parse(src, dest)) {
					incrementService(servIntNetnameBytes, intIP, netname, bytes, serv);
				}
				incrementService(servIntNetnameBytes, intIP, netname, bytes, ALL_SERVICES);
			}
			in.close();
		}
		
		return servIntNetnameBytes;
	}

	private static void incrementService(Map<String, Map<String, Map<String, Double>>> servIntNetnameBytes, String intIP, String netname, double bytes, String serv) {
		Map<String, Map<String, Double>> intNetnameBytes = servIntNetnameBytes.get(serv);
		if (intNetnameBytes == null) {
			intNetnameBytes = CUtil.makeMap();
			servIntNetnameBytes.put(serv, intNetnameBytes);
		}
		
		Map<String, Double> netnameBytes = intNetnameBytes.get(intIP);
		if (netnameBytes == null) {
			netnameBytes = CUtil.makeMap();
			intNetnameBytes.put(intIP, netnameBytes);
		}
		
		Double old = netnameBytes.get(netname);
		if (old == null) {
			old = 0.0;			
		}
		
		netnameBytes.put(netname, old + bytes);
	}
	
	private static Map<String, Map<String, Double>> normalize(Map<String, Map<String, Double>> map) {
		double maxLogBytes = 0.0;
		for (Entry<String, Map<String, Double>> entry : map.entrySet()) {
			for (double bytes : entry.getValue().values()) {
				double logBytes = Math.log(1.0 + bytes);
				if (logBytes > maxLogBytes) {
					maxLogBytes = logBytes;
				}
			}
		}
		
		Map<String, Map<String, Double>> norm = CUtil.makeMap();
		for (Entry<String, Map<String, Double>> entry : map.entrySet()) {
			String intIP = entry.getKey();
			Map<String, Double> netnameWeight = CUtil.makeMap();
			
			for (Entry<String, Double> dayEntry : entry.getValue().entrySet()) {
				String netname = dayEntry.getKey();
				double logBytes = Math.log(1.0 + dayEntry.getValue());
				double weight = logBytes / maxLogBytes;
				netnameWeight.put(netname, weight);
			}
			
			norm.put(intIP, netnameWeight);
		}
		return norm;
	}

}
