package gov.ameslab.cydime.aggregate;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FileUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CydimeRankerAggExpResult {

	public static void main(String[] args) throws IOException {
		int valueIndex = Integer.parseInt(args[0]);
		
		String[] files = new String[] {
			"2015/07/summary_02.csv",
			"2015/07/summary_03.csv",
			"2015/07/summary_04.csv",
			"2015/07/summary_05.csv",
			"2015/07/summary_06.csv",
			"2015/07/summary_07.csv",
			"2015/07/summary_08.csv",
			"2015/07/summary_09.csv",
			"2015/07/summary_10.csv",
			"2015/07/summary_11.csv",
			"2015/07/summary_12.csv",
			"2015/07/summary_13.csv",
			"2015/07/summary_14.csv",
			"2015/07/summary_15.csv",
			"2015/07/summary_16.csv",
			"2015/07/summary_17.csv",
			"2015/07/summary_18.csv",
			"2015/07/summary_19.csv",
			"2015/07/summary_20.csv",
		};
		
		List<String> header = getHeader(files[0]);
		System.out.print("Days");
		for (String h : header) {
			System.out.print(",");
			System.out.print(h);
		}
		System.out.println();
		
		for (int i = 0; i < files.length; i++) {
			Map<String, String> ap = FileUtil.readCSV(files[i], 0, valueIndex, false, true);
			System.out.print(i + 2);
			for (String h : header) {
				System.out.print(",");
				System.out.print(ap.get(h));
			}
			System.out.println();
		}
	}

	private static List<String> getHeader(String file) throws IOException {
		Map<String, String> ap = FileUtil.readCSV(file, 0, 1, false, true);
		
		List<Integer> rankers = CUtil.makeList();
		for (String k : ap.keySet()) {
			rankers.add(Integer.parseInt(k));
		}
		Collections.sort(rankers);
		
		List<String> header = CUtil.makeList();
		for (int r : rankers) {
			header.add(String.valueOf(r));
		}
		return header;
	}
	
}
