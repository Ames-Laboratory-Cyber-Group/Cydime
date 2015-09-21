package gov.ameslab.cydime.aggregate;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CydimeRankerAggExpHostMerge {

	public static void main(String[] args) throws IOException {
		Config.INSTANCE.setParam(args[0]);
		
		int analysisDays = Integer.parseInt(args[1]);
		
		Map<String, String> allIPHost = CUtil.makeMap();
		
		List<Date> dates = Config.INSTANCE.findValidDates(Arrays.asList(Config.INSTANCE.getPath(Config.PREPROCESS_DIR) + "ipHostMap.csv"), analysisDays);
		for (Date d : dates) {
			String file = Config.INSTANCE.getRootPath() + Config.FORMAT_DATE.format(d) + "/" + Config.INSTANCE.getPath(Config.PREPROCESS_DIR) + "ipHostMap.csv";
			Map<String, String> ipHost = FileUtil.readCSV(file, false);
			for (Entry<String, String> entry : ipHost.entrySet()) {
				if ("NA".equals(entry.getValue())) continue;
				
				allIPHost.put(entry.getKey(), entry.getValue());
			}
		}
		
		FileUtil.writeCSV(args[2], allIPHost);
	}
	
}
