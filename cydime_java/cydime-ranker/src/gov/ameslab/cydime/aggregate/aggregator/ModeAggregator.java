package gov.ameslab.cydime.aggregate.aggregator;

import gov.ameslab.cydime.util.HistogramLong;

import java.util.List;

import weka.core.Instance;

public class ModeAggregator extends StringAggregator {

	private HistogramLong<String> cCount;
	
	public ModeAggregator() {
		cCount = new HistogramLong<String>();
	}
	
	@Override
	public String getName() {
		return "mode";
	}
	
	@Override
	public void aggregate(List<Instance> source, int srcIndex, Instance target, int targetIndex) {
		cCount.clear();
		
		for (int i = 0; i < source.size(); i++) {
			Instance inst = source.get(i);
			if (inst.isMissing(srcIndex)) continue;
			
			cCount.increment(inst.stringValue(srcIndex));
		}
		
		String mode = cCount.getMaxKeyByValue();
		if (mode == null) {
			target.setMissing(targetIndex);
		} else {
			target.setValue(targetIndex, mode);
		}
	}

}
