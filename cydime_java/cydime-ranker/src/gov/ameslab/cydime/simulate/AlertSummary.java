package gov.ameslab.cydime.simulate;

import gov.ameslab.cydime.util.CUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Set;

public class AlertSummary {

	public static void main(String[] args) throws IOException {
		Set<String> seen = CUtil.makeSet();
		int[][] counts = new int[20 * 3][3];
		
		BufferedReader in = new BufferedReader(new FileReader(args[0]));
		String line;
		while ((line = in.readLine()) != null) {
			String[] split = line.split(",");
			if (seen.contains(split[2])) continue;
			seen.add(split[2]);
			
			int i = getIndex(split[0]);
			int p = Integer.parseInt(split[1]) - 1;
			counts[i][p]++;
		}
		in.close();
		
		DecimalFormat FORMAT = new DecimalFormat("00");
		for (int i = 0; i < counts.length; i++) {
			System.out.print(FORMAT.format(i / 3) + ":" + FORMAT.format((i % 3) * 20));
			for (int p = 0; p < counts[0].length; p++) {
				System.out.print(",");
				System.out.print(counts[i][p]);
			}
			System.out.println();
		}
	}

	private static int getIndex(String v) {
		String[] split = v.split(":");
		int h = Integer.parseInt(split[0]);
		int m = Integer.parseInt(split[1]);
		m /= 20;
		return h * 3 + m;
	}
	
}
