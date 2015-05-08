package gov.ameslab.cydime.simulate;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class SortedBuffer implements AnalysisBuffer {

	private PriorityQueue<Alert> mBuffer;

	public SortedBuffer() {
		mBuffer = new PriorityQueue<Alert>(11, new Comparator<Alert>() {

			@Override
			public int compare(Alert o1, Alert o2) {
				return Double.compare(o2.getScore(), o1.getScore());
			}
			
		});
	}
	
	@Override
	public String getName() {
		return "Score";
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
			mBuffer.offer(a);
		}
	}

	@Override
	public Alert pop() {
		return mBuffer.poll();
	}

	@Override
	public AnalysisBuffer makeNew() {
		return new SortedBuffer();
	}

}
