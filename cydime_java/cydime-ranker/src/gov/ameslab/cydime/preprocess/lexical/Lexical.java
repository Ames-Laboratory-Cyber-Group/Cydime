package gov.ameslab.cydime.preprocess.lexical;

import gov.ameslab.cydime.model.DomainDatabase;
import gov.ameslab.cydime.model.DomainDatabase.DomainValue;
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
public class Lexical extends FeatureSet {

	private static final Logger Log = Logger.getLogger(Lexical.class.getName());
	
	private DomainDatabase mDomainDB;

	public Lexical(List<String> ids, String inPath, String outPath, DomainDatabase domainDB) {
		super(ids, inPath, outPath);
		mDomainDB = domainDB;
	}
	
	public InstanceDatabase run() throws IOException {
		Log.log(Level.INFO, "Processing lexical similarity...");
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "lexical", ARFFWriter.CLASS_BINARY,
				"avg_sim numeric",
				"var_sim numeric",
				"max_sim numeric"
				); 
		
		String[] values = new String[4];
		values[values.length - 1] = "?";
		for (String id : mIDs) {
			values[0] = toSafeString(mDomainDB.getValue(id, DomainValue.AVERAGE));
			values[1] = toSafeString(mDomainDB.getValue(id, DomainValue.VARIANCE));
			values[2] = toSafeString(mDomainDB.getValue(id, DomainValue.MAX));
			out.writeValues(values);
		}
		
		out.close();
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(mCurrentOutPath, mIDs);
	}

	private String toSafeString(double v) {
		if (Double.isNaN(v)) {
			return "0.0";
		} else {
			return String.valueOf(v);
		}
	}
	
}
