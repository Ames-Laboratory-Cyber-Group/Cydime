package gov.ameslab.cydime.aggregate.aggregator;

import java.util.List;

import weka.core.Instance;

public class MeanAggregator extends NumericAggregator {

	@Override
	public String getName() {
		return "mean";
	}
	
	@Override
	public void aggregate(List<Instance> source, int srcIndex, Instance target, int targetIndex) {
		int count = 0;
		double sum = 0.0;
		
		for (int i = 0; i < source.size(); i++) {
			Instance inst = source.get(i);
			if (inst.isMissing(srcIndex)) continue;
			
			sum += inst.value(srcIndex);
			count++;
		}
		
		if (count == 0) {
			target.setMissing(targetIndex);
		} else {
			target.setValue(targetIndex, sum / count);
		}
	}

}
