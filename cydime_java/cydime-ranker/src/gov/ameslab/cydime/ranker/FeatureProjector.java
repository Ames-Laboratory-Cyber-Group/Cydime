package gov.ameslab.cydime.ranker;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class FeatureProjector extends AbstractClassifier {

	private static final long serialVersionUID = -3922688336586778901L;

	private int mIndex;
	private boolean mDoInvert;

	public FeatureProjector(int index, boolean doInvert) {
		mIndex = index;
		mDoInvert = doInvert;
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
		
		if (mDoInvert) {
			return 1.0 - result;
		} else {
			return result;
		}
	}
	
	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		double p = classifyInstance(instance);
		return new double[] {1.0 - p, p};
	}

	@Override
	public String toString() {
		if (mDoInvert) {
			return mIndex + "-inverted";
		} else {
			return String.valueOf(mIndex);
		}
	}
	
}
