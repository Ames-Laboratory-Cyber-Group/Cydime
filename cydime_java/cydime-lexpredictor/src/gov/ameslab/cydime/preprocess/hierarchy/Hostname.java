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

/**
 * Hostname hierarchy split using dots.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class Hostname extends FeatureSet {

	private static final Logger Log = Logger.getLogger(Hostname.class.getName());
	
	public enum Unit {
		IP,
		ASN
	}
	
	private DomainDatabase mDomainDB;
	private Unit mUnit;

	public Hostname(List<String> ids, String inPath, String outPath, DomainDatabase domainDB, Unit unit) {
		super(ids, inPath, outPath);
		mDomainDB = domainDB;
		mUnit = unit;
	}
	
	public InstanceDatabase run() throws IOException {
		Log.log(Level.INFO, "Processing hostname hierarchy...");
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "hierarchy", null,
				"domain string"
				); 
		
		String[] values = new String[2];
		values[values.length - 1] = "?";
		for (String id : mIDs) {
			String domain = null;
			switch (mUnit) {
			case IP:
				domain = mDomainDB.getDomain(id);
			case ASN:
				List<String> domains = mDomainDB.getDomainsForASN(id);
				if (!domains.isEmpty()) {
					domain = domains.get(0);
				}
			default:					
			}
			
			if (domain == null) {
				values[0] = "'*'";
			} else {
				values[0] = "'" + domain + "'";
			}
		
			out.writeValues(values);
		}
		
		out.close();
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(mCurrentOutPath, mIDs);
	}
	
}
