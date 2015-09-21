package gov.ameslab.cydime.preprocess;

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.NormalizeLog;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Instance;

public class FeatureCombiner {

	private static final Logger Log = Logger.getLogger(FeatureCombiner.class.getName());
	
	private static final DecimalFormat FORMAT = new DecimalFormat("0.000");

	public void run() throws IOException {
		InstanceDatabase base = InstanceDatabase.load(Config.INSTANCE.getBasePath());
		InstanceDatabase derived = makeDerived(base);
		
		Log.log(Level.INFO, "Combining features...");
		InstanceDatabase daily = InstanceDatabase.mergeFeatures(Config.INSTANCE.getDailyPath(),
				base,
				derived
				);		
		
		daily.saveIPs();
		daily.write();
		daily.writeReport();
	}

	private InstanceDatabase makeDerived(InstanceDatabase base) throws IOException {
		Log.log(Level.INFO, "Deriving features...");
		
		ARFFWriter out = new ARFFWriter(Config.INSTANCE.getDerivedPath() + WekaPreprocess.ALL_SUFFIX, "derived", ARFFWriter.CLASS_BINARY,
				"total_records_per_hour numeric",
				"total_bytes_per_hour numeric",
				"total_peercount_per_hour numeric",
				"total_localport_per_hour numeric",
				"total_remoteport_per_hour numeric",
				"ratio_local_remote_port_per_hour numeric"
				); 
		
		for (String id : base.getIDs()) {
			Instance inst = base.getWekaReportInstance(id);
			
			double hours = inst.value(10);
			if (hours < 0.0) hours = 1.0;
			
			double records_h = inst.value(2) / hours;
			double bytes_h = inst.value(3) / hours;
			double peers_h = inst.value(6) / hours;
			double lport_h = inst.value(7) / hours;
			double rport_h = inst.value(8) / hours;
			double lrport_h = inst.value(9) / hours;
			
			out.writeValues(FORMAT.format(records_h),
					FORMAT.format(bytes_h),
					FORMAT.format(peers_h),
					FORMAT.format(lport_h),
					FORMAT.format(rport_h),
					FORMAT.format(lrport_h),
					"?");
		}
		
		out.close();
		
		FileUtil.copy(Config.INSTANCE.getDerivedPath() + WekaPreprocess.ALL_SUFFIX, Config.INSTANCE.getDerivedPath() + WekaPreprocess.REPORT_SUFFIX);
		
		WekaPreprocess.filterUnsuperivsed(Config.INSTANCE.getDerivedPath() + WekaPreprocess.ALL_SUFFIX,
				NormalizeLog);
		
		return new InstanceDatabase(Config.INSTANCE.getDerivedPath(), base.getIDs());
	}

}
