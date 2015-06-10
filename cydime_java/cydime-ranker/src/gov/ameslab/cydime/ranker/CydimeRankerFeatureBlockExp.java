/*
 * Copyright (c) 2014 Iowa State University
 * All rights reserved.
 * 
 * Copyright 2014.  Iowa State University.  This software was produced under U.S.
 * Government contract DE-AC02-07CH11358 for The Ames Laboratory, which is 
 * operated by Iowa State University for the U.S. Department of Energy.  The U.S.
 * Government has the rights to use, reproduce, and distribute this software.
 * NEITHER THE GOVERNMENT NOR IOWA STATE UNIVERSITY MAKES ANY WARRANTY, EXPRESS
 * OR IMPLIED, OR ASSUMES ANY LIABILITY FOR THE USE OF THIS SOFTWARE.  If 
 * software is modified to produce derivative works, such modified software 
 * should be clearly marked, so as not to confuse it with the version available
 * from The Ames Laboratory.  Additionally, redistribution and use in source and
 * binary forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 * 
 * 1.  Redistribution of source code must retain the above copyright notice, this
 * list of conditions, and the following disclaimer.
 * 2.  Redistribution in binary form must reproduce the above copyright notice, 
 * this list of conditions, and the following disclaimer in the documentation 
 * and/or other materials provided with distribution.
 * 3.  Neither the name of Iowa State University, The Ames Laboratory, the
 * U.S. Government, nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission
 * 
 * THIS SOFTWARE IS PROVIDED BY IOWA STATE UNIVERSITY AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL IOWA STATE UNIVERSITY OF CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITRY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */

package gov.ameslab.cydime.ranker;

import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.model.ListDatabase;
import gov.ameslab.cydime.ranker.evaluate.AveragePrecision;
import gov.ameslab.cydime.ranker.evaluate.NDCG;
import gov.ameslab.cydime.ranker.evaluate.RankEvaluator;
import gov.ameslab.cydime.ranker.evaluate.RankerResultList;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Cydime Predictor experimenter using a set of representative predictors.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeRankerFeatureBlockExp {

	private static final Logger Log = Logger.getLogger(CydimeRankerFeatureBlockExp.class.getName());

	public static final int RUNS = 10;
	public static final double TRAIN_PERCENT = 100.0 * 2 / 3;
	private static final DecimalFormat LABEL_FORMAT = new DecimalFormat("0");
	
	private AbstractClassifier[] mAlgorithms = new AbstractClassifier[] {
			RankerFactory.makeNaiveBayes(),
			RankerFactory.makeLogistic(),
			RankerFactory.makeAdaBoostM1(),
	};
	
	private int[][] mFeatures = new int[][] {
			{0,2,3,4,5,6,7,8,9,15,16,17,18,19,20,21,22,23, 33}, //Flow
			{10,11,12,13,14,24,25,26,27,28,29, 33}, //Temporal
			{1, 33}, //Location
			{30,31,32, 33}, //Semantic
			{0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29, 33}, //Flow+Temporal
			{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29, 33}, //Flow+Temporal+Location
	};
	
	private RankEvaluator[] mEvaluators = new RankEvaluator[] {
			new AveragePrecision(),
			new NDCG(),
	};

	private double mLabelPercentage;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			printUsage();
			return;
		}

		new CydimeRankerFeatureBlockExp(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimeRankerExp FEATURE_DIR LABEL_PERCENT");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
		System.out.println("    LABEL_PERCENT: [1,99] percentage of labeled instances");
	}

	public CydimeRankerFeatureBlockExp(String[] args) {
		Config.INSTANCE.setParam(args[0]);
		Config.INSTANCE.setFeatureSet(Config.FEATURE_IP_DIR);

		mLabelPercentage = Double.parseDouble(args[1]);
	}

	private void run() throws Exception {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		
		List<String> ips = CUtil.makeList(baseNorm.getIDs());
		
		ListDatabase whiteDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_WHITE_FILE));
		LabelSample whiteLabel = new LabelSample(whiteDB.getList(ips));
		
		ListDatabase blackDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_BLACK_FILE));
		LabelSample blackLabel = new LabelSample(blackDB.getList(ips));
		
		for (int run = 0; run < RUNS; run++) {
			List<String> whiteSample = whiteLabel.getNextSample();
			List<String> blackSample = blackLabel.getNextSample();
			LabelSplit split = new LabelSplit(ips, whiteSample, blackSample, TRAIN_PERCENT, mLabelPercentage, new Random(run));
			runU0(run, split);
		}
		
		summarize("U0");
	}

	private Remove getFilter(int[] fs) {
		Remove filter = new Remove();
		filter.setInvertSelection(true);
		filter.setAttributeIndicesArray(fs);
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
	
	private void runU0(int runID, LabelSplit split) throws Exception {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		String labelAnnot = "_U0label" + LABEL_FORMAT.format(mLabelPercentage);
		
		Instances wekaTrain = getTrain(baseNorm, split);
		Instances wekaTest = getTest(baseNorm, split);
		
		for (int f = 0; f < mFeatures.length; f++) {
			Log.log(Level.INFO, "Preparing feature set {0}...", f);
			
			Instances wekaTrainSub = null;
			Instances wekaTestSub = null;
			
			Remove filter = getFilter(mFeatures[f]);		
			try {
				filter.setInputFormat(wekaTrain);
				wekaTrainSub = Filter.useFilter(wekaTrain, filter);
				wekaTestSub = Filter.useFilter(wekaTest, filter);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			for (int i = 0; i < mAlgorithms.length; i++) {
				Log.log(Level.INFO, "Building {0}...", i);
				
				mAlgorithms[i].buildClassifier(wekaTrainSub);
				
				Map<String, Double> preds = CUtil.makeMap();
				List<String> testKnown = split.getTestKnown();
				for (int t = 0; t < testKnown.size(); t++) {
					String ip = testKnown.get(t);
					Instance inst = wekaTestSub.get(t);
					double dist[] = mAlgorithms[i].distributionForInstance(inst);
					preds.put(ip, dist[1]);
				}
				
				writeScore(getScoreFile(runID, labelAnnot, f, i), split.getTestWhite(), preds);
			}
		}
	}
	
	private String getScoreFile(int runID, String labelAnnot, int fID, int algID) {
		return Config.INSTANCE.getCurrentReportPath() + "score" + labelAnnot + "_run" + runID + "_set" + fID + "_ranker" + algID + ".csv";
	}

	private void writeScore(String file, List<String> testWhite, Map<String, Double> preds) throws IOException {
		List<String> rank = CUtil.getSortedKeysByValue(preds);
		Collections.reverse(rank);
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		for (int e = 0; e < mEvaluators.length; e++) {
			double score = mEvaluators[e].evaluate(testWhite, rank);
			out.write(mEvaluators[e].getName() + "," + score);
			out.newLine();
		}
		out.close();
	}

	private void summarize(String prefix) throws IOException {
		String labelAnnot = "_" + prefix + "label" + LABEL_FORMAT.format(mLabelPercentage);
		
		for (int f = 0; f < mFeatures.length; f++) {
			RankerResultList results = new RankerResultList(mAlgorithms.length, mEvaluators.length, RUNS);
			
			for (int i = 0; i < mAlgorithms.length; i++) {
				for (int run = 0; run < RUNS; run++) {
					results.addRun(i, run, getScoreFile(run, labelAnnot, f, i));
				}
			}
			
			results.write(Config.INSTANCE.getCurrentReportPath() + "summary" + labelAnnot + "_set" + f + ".csv", mEvaluators);
		}
	}

}
