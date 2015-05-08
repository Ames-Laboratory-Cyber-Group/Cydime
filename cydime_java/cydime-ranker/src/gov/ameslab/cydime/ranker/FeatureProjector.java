package gov.ameslab.cydime.ranker;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class FeatureProjector extends AbstractClassifier {

	private static final long serialVersionUID = -3922688336586778901L;

	private int mIndex;

	public FeatureProjector(int index) {
		mIndex = index;
	}

	@Override
	public void buildClassifier(Instances instances) throws Exception {
	}

	@Override
	public double classifyInstance(Instance instance) throws Exception {
		double result = instance.value(mIndex);
		if (Double.isNaN(result)) {
			result = 0.0;
		}
		return result;
	}
	
	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		double p = classifyInstance(instance);
		return new double[] {1.0 - p, p};
	}
	
}
