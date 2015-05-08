package gov.ameslab.cydime.simulate;

import gov.ameslab.cydime.util.CUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ComplaintFirstBuffer {

	private AnalysisBuffer mBuffer;
	private Set<String> mTrueWhite;
	private long mComplaintThreshold;

	private LinkedList<Alert> mOpenWhiteAlerts;
	private Set<Alert> mClosedWhiteAlerts;
	
	public ComplaintFirstBuffer(AnalysisBuffer buffer, Set<String> trueWhite, long comp) {
		mBuffer = buffer;
		mTrueWhite = trueWhite;
		mComplaintThreshold = comp;
		mOpenWhiteAlerts = new LinkedList<Alert>();
		mClosedWhiteAlerts = CUtil.makeSet();
	}

	public String getName() {
		return mBuffer.getName();
	}

	public boolean isEmpty() {
		return mBuffer.isEmpty();
	}

	public void insert(List<Alert> alerts) {
		mBuffer.insert(alerts);
		
		for (Alert a : alerts) {
			if (a.isMemberOf(mTrueWhite)) {
				mOpenWhiteAlerts.addLast(a);
			}
		}
	}

	public Alert pop(long clock) {
		if (!mOpenWhiteAlerts.isEmpty()) {
			if (mOpenWhiteAlerts.getFirst().exceedsThreshold(clock, mComplaintThreshold)) {
				Alert first = mOpenWhiteAlerts.removeFirst();
				mClosedWhiteAlerts.add(first);
				return first;
			}
		}
		
		Alert pop = mBuffer.pop();
		while (pop != null && mClosedWhiteAlerts.contains(pop)) {
			pop = mBuffer.pop();
		}
		return pop;
	}

}
