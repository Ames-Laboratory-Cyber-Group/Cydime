package gov.ameslab.cydime.ranker;

import java.util.Random;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class RandomRanker extends AbstractClassifier {

	private static final long serialVersionUID = 9052795906322104737L;

	private Random mRandom;

	public RandomRanker() {
		mRandom = new Random(0);
	}
	
	@Override
	public void buildClassifier(Instances instances) throws Exception {
	}

	@Override
	public double classifyInstance(Instance instance) throws Exception {
		return mRandom.nextDouble();
	}
	
	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		double p = classifyInstance(instance);
		return new double[] {1.0 - p, p};
	}

}
