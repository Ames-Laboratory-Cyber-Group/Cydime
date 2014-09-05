package gov.ameslab.cydime.preprocess.hierarchy;

import gov.ameslab.cydime.model.DomainDatabase;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.FileUtil;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Hostname extends FeatureSet {

	private static final Logger Log = Logger.getLogger(Hostname.class.getName());
	
	private DomainDatabase mDomainDB;

	public Hostname(List<String> allIPs, String inPath, String outPath, DomainDatabase domainDB) {
		super(allIPs, inPath, outPath);
		mDomainDB = domainDB;
	}
	
	public InstanceDatabase run() throws IOException {
		Log.log(Level.INFO, "Processing hostname hierarchy...");
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "hierarchy", null,
				"domain string"
				); 
		
		String[] values = new String[2];
		values[values.length - 1] = "?";
		for (String ip : mAllIPs) {
			String domain = mDomainDB.getDomain(ip);
			if (domain == null) {
				values[0] = "'*'";
			} else {
				values[0] = "'" + domain + "'";
			}
		
			out.writeValues(values);
		}
		
		out.close();
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(mCurrentOutPath, mAllIPs);
	}
	
}
