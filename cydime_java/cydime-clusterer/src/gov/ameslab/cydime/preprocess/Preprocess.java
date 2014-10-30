package gov.ameslab.cydime.preprocess;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class Preprocess {

	public static Map<String, String> readWhois() throws IOException {
		Map<String, String> ipNetname = FileUtil.readCSV(Config.IP_WHOIS_FILE, 0, 1, true, false);
		for (String ip : CUtil.makeSet(ipNetname.keySet())) {
			String whois = ipNetname.get(ip);
			if ("NA".equalsIgnoreCase(whois)) {
				ipNetname.remove(ip);
			} else {
				String[] split = whois.split("\\.");
				ipNetname.put(ip, split[1] + ":" + split[0]);
			}
		}
		
		return ipNetname;
	}
	
	public static void writeGraph(String file, Map<String, Map<String, Double>> map) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.BIGRAPH_PATH + file + ".csv"));
		for (Entry<String, Map<String, Double>> entry : map.entrySet()) {
			String intIP = entry.getKey();
			for (Entry<String, Double> dayEntry : entry.getValue().entrySet()) {
				String netname = dayEntry.getKey();
				double weight = dayEntry.getValue();
				out.write(intIP);
				out.write(",");
				out.write(netname);
				out.write(",");
				out.write(String.valueOf(weight));
				out.newLine();
			}
		}
		out.close();
	}

}
