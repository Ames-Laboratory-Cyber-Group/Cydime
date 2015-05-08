package gov.ameslab.cydime.simulate;

import gov.ameslab.cydime.util.CUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlertStream {

	private List<Alert> mAlerts;
	private int mLastIndex;
	
	private List<Alert> tAdvance;
	
	public AlertStream(String file, Map<String, Double> ipScore) throws IOException {
		mAlerts = CUtil.makeList();
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		while ((line = in.readLine()) != null) {
			String[] split = line.split(",");
			long time = getTime(split[0]);
			String ip = split[1];
			Double score = ipScore.get(ip);
			if (score == null) score = 0.0;
			Alert a = new Alert(time, ip, score);
			mAlerts.add(a);
		}
		
		Collections.sort(mAlerts, new Comparator<Alert>() {

			@Override
			public int compare(Alert o1, Alert o2) {
				return Long.compare(o1.getTimestamp(), o2.getTimestamp());
			}
			
		});
		
		in.close();
	}

	private static long getTime(String str) {
		String[] split = str.split(":");
		int h = Integer.parseInt(split[0]);
		int m = Integer.parseInt(split[1]);
		int s = Integer.parseInt(split[2]);
		return h * 3600 + m * 60 + s;
	}

	public Set<String> getIPs() {
		Set<String> ips = CUtil.makeSet();
		for (Alert a : mAlerts) {
			ips.add(a.getIP());
		}
		return ips;
	}

	public void reset() {
		mLastIndex = 0;
		tAdvance = CUtil.makeList();
	}

	public boolean isEmpty() {
		return mLastIndex >= mAlerts.size();
	}
	
	public List<Alert> advance(long clock) {
		tAdvance.clear();
		while (mLastIndex < mAlerts.size()) {
			Alert a = mAlerts.get(mLastIndex);
			if (a.getTimestamp() <= clock) {
				tAdvance.add(a);
				mLastIndex++;
			} else {
				break;
			}
		}
		
		return tAdvance;
	}


}
