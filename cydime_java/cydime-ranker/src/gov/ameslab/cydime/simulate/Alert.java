package gov.ameslab.cydime.simulate;

import java.util.Set;

public class Alert {

	private long mTimestamp;
	private String mIP;
	private double mScore;

	public Alert(long time, String ip, double score) {
		mTimestamp = time;
		mIP = ip;
		mScore = score;
	}

	public String getIP() {
		return mIP;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public long getAnalyzeTime(long clock) {
		return clock - mTimestamp;
	}

	public double getScore() {
		return mScore;
	}
	
	@Override
	public String toString() {
		return String.valueOf(mScore);
	}

	public boolean exceedsThreshold(long clock, long comp) {
		long aTime = getAnalyzeTime(clock);
		return aTime > comp;
	}

	public boolean isMemberOf(Set<String> ips) {
		return ips.contains(mIP);
	}
	
}
