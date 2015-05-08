package gov.ameslab.cydime.simulate;

import java.util.LinkedList;
import java.util.List;

public class FIFOBuffer implements AnalysisBuffer {

	private LinkedList<Alert> mBuffer;

	public FIFOBuffer() {
		mBuffer = new LinkedList<Alert>();
	}
	
	@Override
	public String getName() {
		return "FIFO";
	}

	@Override
	public int size() {
		return mBuffer.size();
	}

	@Override
	public boolean isEmpty() {
		return mBuffer.isEmpty();
	}

	@Override
	public void insert(List<Alert> alerts) {
		for (Alert a : alerts) {
			mBuffer.addLast(a);
		}
	}

	@Override
	public Alert pop() {
		if (mBuffer.isEmpty()) return null;
		return mBuffer.removeFirst();
	}

	@Override
	public AnalysisBuffer makeNew() {
		return new FIFOBuffer();
	}

}
