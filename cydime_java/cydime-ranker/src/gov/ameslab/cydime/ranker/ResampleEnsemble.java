package gov.ameslab.cydime.ranker;

import gov.ameslab.cydime.util.CUtil;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class ResampleEnsemble extends AbstractClassifier {

	private static final long serialVersionUID = 3994131108092278612L;
	
	private List<AbstractClassifier> mBases;
	private double mNegOverPos;
	private Random mRandom;

	public ResampleEnsemble(List<AbstractClassifier> bases, double negOverPos, Random seed) {
		mBases = bases;
		mNegOverPos = negOverPos;
		mRandom = seed;
	}

	@Override
	public void buildClassifier(Instances instances) throws Exception {
		List<Instance> positives = CUtil.makeList();
		List<Instance> negatives = CUtil.makeList();
		for (int j = 0; j < instances.size(); j++) {
			Instance inst = instances.get(j);
			if (inst.classIsMissing()) continue;
			
			String c = inst.stringValue(inst.classIndex());
			if (LabelSplit.LABEL_POSITIVE.equals(c)) {
				positives.add(inst);
			} else if (LabelSplit.LABEL_NEGATIVE.equals(c)) {
				negatives.add(inst);
			}
		}
		
		int cutoff = (int) (positives.size() * mNegOverPos);
		
		for (int i = 0; i < mBases.size(); i++) {
			Instances resample = new Instances(instances, 0);
			
			for (Instance inst : positives) {
				inst.setDataset(resample);
				resample.add(inst);
			}
			
			Collections.shuffle(negatives, mRandom);
			for (Instance inst : negatives.subList(0, cutoff)) {
				inst.setDataset(resample);
				resample.add(inst);
			}
			
			mBases.get(i).buildClassifier(resample);
		}
	}
	
	@Override
	public double classifyInstance(Instance instance) throws Exception {
		double sum = 0.0;
		int count = 0;
		for (AbstractClassifier c : mBases) {
			double[] pred = c.distributionForInstance(instance);
			if (Double.isNaN(pred[1])) continue;
			
			sum += pred[1];
			count++;
		}
		
		if (count == 0) return 0.0;
		else return sum / count;
	}

	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		double p = classifyInstance(instance);
		return new double[] {1.0 - p, p};
	}
	
}
