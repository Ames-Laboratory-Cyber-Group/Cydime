package gov.ameslab.cydime.preprocess.hierarchy;

import gov.ameslab.cydime.model.DomainDatabase;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.util.CUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HostnameCC extends FeatureSet {

	private static final Logger Log = Logger.getLogger(HostnameCC.class.getName());
	
	private DomainDatabase mDomainDB;

	public HostnameCC(List<String> allIPs, String inPath, String outPath, DomainDatabase domainDB) {
		super(allIPs, inPath, outPath);
		mDomainDB = domainDB;
	}
	
	public InstanceDatabase run() throws IOException {
		Log.log(Level.INFO, "Processing hostname hierarchy...");
		
		Map<String, String> ipCC = readCC();
		Map<String, String> ccHierarchy = readCCHierarchy();
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "hierarchy", null,
				"domain string",
				"cc string"
				); 
		
		String[] values = new String[3];
		values[values.length - 1] = "?";
		for (String ip : mAllIPs) {
			String domain = mDomainDB.getDomain(ip);
			if (domain == null) {
				values[0] = "'*'";
			} else {
				values[0] = "'" + domain + "'";
			}
		
			String cc = ipCC.get(ip);
			String ccPath = ccHierarchy.get(cc);
			if (ccPath == null) {
				values[1] = "'*'";
			} else {
				values[1] = "'" + ccPath + "'";
			}			
			
			out.writeValues(values);
		}
		
		out.close();
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(mCurrentOutPath, mAllIPs);
	}

	private Map<String, String> readCC() throws IOException {
		Map<String, String> ipCC = CUtil.makeMap();
		
		BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getCurrentFeaturePath() + Config.INSTANCE.getNetflow()));
		String line = in.readLine();		
		while ((line = in.readLine()) != null) {
			String[] values = line.toLowerCase().split(",");
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].trim();
			}
			String ip = values[0];
			
			//src_cc,dst_cc
			if (values[1].equals("--") && values[2].equals("--")) {
				//
			} else if (values[1].equals("--")) {
				ipCC.put(ip, values[2]);
			} else {
				ipCC.put(ip, values[1]);
			}
		}
		
		in.close();
		return ipCC;
	}
	
	private Map<String, String> readCCHierarchy() throws IOException {
		Map<String, String> ccHierarchy = CUtil.makeMap();
		
		BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getRootPath() + "labels/cc_hierarchy.csv"));
		String line = in.readLine();		
		while ((line = in.readLine()) != null) {
			String[] values = line.toLowerCase().split(",");
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].trim();
			}
			
			ccHierarchy.put(values[0], values[0] + "." + values[1]);
		}
		
		in.close();		
		return ccHierarchy;
	}

}
