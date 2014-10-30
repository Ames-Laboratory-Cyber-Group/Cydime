package gov.ameslab.cydime.preprocess;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DayAccessFilter {

	private static final int OUTPUT_SIZE = 100000;
	
	private static Calendar mCal = GregorianCalendar.getInstance();
	private static Set<String> mActiveInts;
	
	public static void main(String[] args) throws IOException {
		mActiveInts = CUtil.makeSet(FileUtil.readFile(Config.ACTIVE_INTERNAL_FILE, true));
		
		Map<String, Set<Integer>> ipDays = CUtil.makeMap();
		for (String file : args) {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				if (!mActiveInts.contains(split[0])) continue;
				
				String ip = split[1];
				Set<Integer> days = ipDays.get(ip);
				if (days == null) {
					days = CUtil.makeSet();
					ipDays.put(ip, days);
				}
				
				long epoch = Long.parseLong(split[4]) * 1000;
				Date date = new Date(epoch);
				int dayOfYear = getDayOfYear(date);
				days.add(dayOfYear);
			}
			in.close();
		}
		
		Map<String, Double> ipDayCount = CUtil.makeMap();
		for (Entry<String, Set<Integer>> entry : ipDays.entrySet()) {
			ipDayCount.put(entry.getKey(), (double) entry.getValue().size());
		}
		
		List<String> ipSorted = CUtil.getSortedKeysByValue(ipDayCount);
		int begin = Math.max(0, ipSorted.size() - OUTPUT_SIZE);
		
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.ACTIVE_EXTERNAL_FILE));
		for (String ip : ipSorted.subList(begin, ipSorted.size())) {
			out.write(ip);
			out.newLine();
		}
		out.close();
	}
	
	private static int getDayOfYear(Date date) {
		mCal.setTime(date);
		return mCal.get(Calendar.DAY_OF_YEAR);
	}
	
}
