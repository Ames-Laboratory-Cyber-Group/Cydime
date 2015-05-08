package gov.ameslab.cydime.simulate;

public class SimulationResult {

	private int mComplaintCount;
	private int mThresholdCount;

	public int getComplaintCount() {
		return mComplaintCount;
	}
	
	public int getThresholdCount() {
		return mThresholdCount;
	}
	
	public void setComplaintCount(int count) {
		mComplaintCount = count;
	}

	public void setThresholdCount(int count) {
		mThresholdCount = count;
	}

}
