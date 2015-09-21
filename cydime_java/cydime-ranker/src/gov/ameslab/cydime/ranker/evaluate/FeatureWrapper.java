package gov.ameslab.cydime.ranker.evaluate;

import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.ranker.LabelSplit;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class FeatureWrapper {

	private static final Logger Log = Logger.getLogger(FeatureWrapper.class.getName());
	
	private AbstractClassifier mClassifier;
	private RankEvaluator mEvaluator;
	private LabelSplit[] mSplits;
	private int mLastFeatureIndex;

	private Set<Integer> mFeatures;
	private double mLastEval;
	private int mLastFeature;

	private InstanceDatabase cBaseNorm;
	private Instances[] mTrains;
	private Instances[] mTests;
	
	public FeatureWrapper(AbstractClassifier c, RankEvaluator e, LabelSplit[] splits, int totalFeatures) {
		mClassifier = c;
		mEvaluator = e;
		mSplits = splits;
		mLastFeatureIndex = totalFeatures - 1;
		mFeatures = CUtil.makeSet();
	}

	public boolean findNext() throws Exception {
		if (cBaseNorm == null) {
			cBaseNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
			mTrains = new Instances[mSplits.length];
			mTests = new Instances[mSplits.length];
			
			for (int i = 0; i < mSplits.length; i++) {
				mTrains[i] = getTrain(cBaseNorm, mSplits[i]);
				mTests[i] = getTest(cBaseNorm, mSplits[i]);
			}
		}
		
		int bestFeatureIndex = -1;
		double bestFeatureEval = 0.0;
		
		for (int i = 0; i <= mLastFeatureIndex; i++) {
			if (mFeatures.contains(i)) continue;
			
//			if (i == 1 || i == 30 || i == 31 || i == 32) continue; //TEST
			
			double eval = wrapWithFeature(i);
			if (eval > bestFeatureEval) {
				bestFeatureEval = eval;
				bestFeatureIndex = i;
			}
			
			Log.log(Level.INFO, "Wrapped feature " + i + " = " + eval);
		}
			
		boolean result = (bestFeatureEval > mLastEval);
		
		Log.log(Level.INFO, "Adding feature {0}...", bestFeatureIndex);
		
		mFeatures.add(bestFeatureIndex);
		mLastEval = bestFeatureEval;
		mLastFeature = bestFeatureIndex;
		
		return result;
	}

	private double wrapWithFeature(int index) throws Exception {
		Remove filter = getFilter(index);		
		
		double evalSum = 0.0;
		for (int i = 0; i < mSplits.length; i++) {
			Instances wekaTrain = null;
			Instances wekaTest = null;
			try {
				filter.setInputFormat(mTrains[i]);
				wekaTrain = Filter.useFilter(mTrains[i], filter);
				wekaTest = Filter.useFilter(mTests[i], filter);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			mClassifier.buildClassifier(wekaTrain);
			
			Map<String, Double> preds = CUtil.makeMap();
			List<String> testKnown = mSplits[i].getTestKnown();
			for (int t = 0; t < testKnown.size(); t++) {
				String ip = testKnown.get(t);
				Instance inst = wekaTest.get(t);
				double dist[] = mClassifier.distributionForInstance(inst);
				preds.put(ip, dist[1]);
			}
			
			List<String> rank = CUtil.getSortedKeysByValue(preds);
			Collections.reverse(rank);
			
			evalSum += mEvaluator.evaluate(mSplits[i].getTestWhite(), rank);
		}
				
		return evalSum / mSplits.length;
	}

	private Remove getFilter(int index) {
		List<Integer> selectedFeatures = CUtil.makeList(mFeatures);
		selectedFeatures.add(index);
		selectedFeatures.add(mLastFeatureIndex + 1);
		Collections.sort(selectedFeatures);
		int[] as = new int[selectedFeatures.size()];
		for (int i = 0; i < as.length; i++) {
			as[i] = selectedFeatures.get(i);
		}
		
		Remove filter = new Remove();
		filter.setInvertSelection(true);
		filter.setAttributeIndicesArray(as);
		return filter;
	}

	private Instances getTrain(InstanceDatabase baseNorm, LabelSplit split) {
		Instances train = new Instances(baseNorm.getWekaInstances(), 0);
		
		for (String ip : split.getTrainWhite()) {
			Instance inst = baseNorm.getWekaInstance(ip);
			inst.setClassValue(LabelSplit.LABEL_POSITIVE);
			inst.setDataset(train);
			train.add(inst);
		}
		
//		for (String ip : split.getTrainBlack()) {
		for (String ip : split.getTrainNonWhite()) {
			Instance inst = baseNorm.getWekaInstance(ip);
			inst.setClassValue(LabelSplit.LABEL_NEGATIVE);
			inst.setDataset(train);
			train.add(inst);
		}
		
		return train;
	}

	private Instances getTest(InstanceDatabase baseNorm, LabelSplit split) {
		Instances test = new Instances(baseNorm.getWekaInstances(), 0);
		for (String ip : split.getTestKnown()) {
			Instance inst = baseNorm.getWekaInstance(ip);
			inst.setDataset(test);
			test.add(inst);
		}
		return test;
	}

	public int getFeatureSize() {
		return mFeatures.size();
	}

	public double getLastEval() {
		return mLastEval;
	}
	
	public int getLastFeature() {
		return mLastFeature;
	}

}
