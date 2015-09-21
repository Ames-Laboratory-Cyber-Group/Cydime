package gov.ameslab.cydime.aggregate.aggregator;

import java.util.List;

import weka.core.Instance;


public class VarianceAggregator extends NumericAggregator {

	@Override
	public String getName() {
		return "variance";
	}

	@Override
	public void aggregate(List<Instance> source, int srcIndex, Instance target, int targetIndex) {
		int count = 0;
		double sum = 0.0;
		double sumsq = 0.0;
		
		for (int i = 0; i < source.size(); i++) {
			Instance inst = source.get(i);
			if (inst.isMissing(srcIndex)) continue;
			
			double value = inst.value(srcIndex);
			sum += value;
			sumsq += value * value;
			count++;
		}
		
		if (count == 0) {
			target.setMissing(targetIndex);
		} else {
			double sqmean = sumsq / count;
			double mean = sum / count;
			target.setValue(targetIndex, sqmean - mean * mean);
		}
	}

}
