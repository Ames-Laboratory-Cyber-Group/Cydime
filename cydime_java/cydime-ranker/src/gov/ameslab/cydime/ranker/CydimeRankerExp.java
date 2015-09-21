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
import gov.ameslab.cydime.preprocess.WekaPreprocess;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Cydime Predictor experimenter using a set of representative predictors.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeRankerExp {

	private static final Logger Log = Logger.getLogger(CydimeRankerExp.class.getName());

	public static final int RUNS = 10;
	public static final double TRAIN_PERCENT = 100.0 * 2 / 3;
	private static final DecimalFormat LABEL_FORMAT = new DecimalFormat("0");
	
	private AbstractClassifier[] mAlgorithms;
	private AbstractClassifier[] mAlgorithmsU;
	
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

		new CydimeRankerExp(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimeRankerExp FEATURE_DIR LABEL_PERCENT");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
		System.out.println("    LABEL_PERCENT: [1,99] percentage of labeled instances");
	}

	public CydimeRankerExp(String[] args) {
		Config.INSTANCE.setParam(args[0]);
		Config.INSTANCE.setFeatureDir(Config.IP_DIR);

		mLabelPercentage = Double.parseDouble(args[1]);
		
		List<AbstractClassifier> algorithms = CUtil.makeList();
		algorithms.add(RankerFactory.makeRandomRanker());
		algorithms.add(RankerFactory.makeNaiveBayes());
		algorithms.add(RankerFactory.makeLogistic());
		algorithms.add(RankerFactory.makeREPTree());
		algorithms.add(RankerFactory.makeLibSVM());
		algorithms.add(RankerFactory.makeRandomForest());			
		algorithms.add(RankerFactory.makeAdaBoostM1());
		for (int i = 2; i <= 41; i++) {
			algorithms.add(RankerFactory.makeFeatureProjector(i, false));
			algorithms.add(RankerFactory.makeFeatureProjector(i, true));
		}
		mAlgorithms = new AbstractClassifier[algorithms.size()];
		algorithms.toArray(mAlgorithms);
		
		List<AbstractClassifier> algorithmsU = CUtil.makeList();
		algorithmsU.add(RankerFactory.makeRandomRanker());
		algorithmsU.add(RankerFactory.makeNaiveBayes());
		algorithmsU.add(RankerFactory.makeLogistic());
		algorithmsU.add(RankerFactory.makeREPTree());
//		algorithms.add(RankerFactory.makeLibSVM());
		algorithmsU.add(RankerFactory.makeResampleEnsemble(RankerFactory.makeLibSVM(10), 1.0, new Random(1)));
		algorithmsU.add(RankerFactory.makeRandomForest());			
		algorithmsU.add(RankerFactory.makeAdaBoostM1());
		for (int i = 2; i <= 41; i++) {
			algorithmsU.add(RankerFactory.makeFeatureProjector(i, false));
			algorithmsU.add(RankerFactory.makeFeatureProjector(i, true));
		}
		mAlgorithmsU = new AbstractClassifier[algorithmsU.size()];
		algorithmsU.toArray(mAlgorithmsU);	
	}

	private void run() throws Exception {
		InstanceDatabase aggNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
		
		List<String> ips = CUtil.makeList(aggNorm.getIDs());
		
		ListDatabase whiteDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_WHITE_FILE));
		LabelSample whiteLabel = new LabelSample(whiteDB.getList(ips));
		
		ListDatabase blackDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_BLACK_FILE));
		LabelSample blackLabel = new LabelSample(blackDB.getList(ips));
		
		printStats(ips, whiteDB.getList(ips), blackDB.getList(ips));
		
		for (int run = 0; run < RUNS; run++) {
			List<String> whiteSample = whiteLabel.getNextSample();
			List<String> blackSample = blackLabel.getNextSample();
			LabelSplit split = new LabelSplit(ips, whiteSample, blackSample, TRAIN_PERCENT, mLabelPercentage, new Random(run));
			run00(run, split);
			runU0(run, split);
			runUU(run, split);
		}
		
		summarize("00");
		summarize("U0");
		summarize("UU");
	}

	private void printStats(List<String> ips, List<List<String>> white, List<List<String>> black) {
		System.out.println("IP = " + ips.size());
		System.out.println("White subnets = " + white.size());
		System.out.println("Black subnets = " + black.size());
		
		int subnets = 0;
		int sum = 0;
		for (List<String> list : white) {
			subnets++;
			sum += list.size();
		}
		for (List<String> list : black) {
			subnets++;
			sum += list.size();
		}
		System.out.println("Average = " + (1.0 * sum / subnets));
	}

	private void run00(int runID, LabelSplit split) throws Exception {
		InstanceDatabase aggNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
		String labelAnnot = "_00label" + LABEL_FORMAT.format(mLabelPercentage);
		
		for (String ip : split.getTrainWhite()) {
			aggNorm.setLabel(ip, LabelSplit.LABEL_POSITIVE);
		}
		
		for (String ip : split.getTrainBlack()) {
			aggNorm.setLabel(ip, LabelSplit.LABEL_NEGATIVE);
		}

		Instances wekaTrain = aggNorm.getWekaTrain();
		WekaPreprocess.save(wekaTrain, Config.INSTANCE.getCurrentReportPath() + "00train" + runID + ".arff");
		
		for (int i = 0; i < mAlgorithms.length; i++) {
			Log.log(Level.INFO, "Building {0}...", i);
			
			mAlgorithms[i].buildClassifier(wekaTrain);
			
			Map<String, Double> preds = CUtil.makeMap();
			for (String ip : split.getTestKnown()) {
				Instance inst = aggNorm.getWekaInstance(ip);
				double dist[] = mAlgorithms[i].distributionForInstance(inst);
				preds.put(ip, dist[1]);
			}
			
			writeRank(getResultFile(runID, labelAnnot, i), split.getTestKnown(), split.getTestWhite(), preds);
			writeScore(getScoreFile(runID, labelAnnot, i), split.getTestWhite(), preds);
		}
	}

	private void runU0(int runID, LabelSplit split) throws Exception {
		InstanceDatabase aggNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
		String labelAnnot = "_U0label" + LABEL_FORMAT.format(mLabelPercentage);
		
		for (String ip : split.getTrainWhite()) {
			aggNorm.setLabel(ip, LabelSplit.LABEL_POSITIVE);
		}
		
		for (String ip : split.getTrainNonWhite()) {
			aggNorm.setLabel(ip, LabelSplit.LABEL_NEGATIVE);
		}

		Instances wekaTrain = aggNorm.getWekaTrain();
		for (int i = 0; i < mAlgorithmsU.length; i++) {
			Log.log(Level.INFO, "Building {0}...", i);
			
			mAlgorithmsU[i].buildClassifier(wekaTrain);
			
			Map<String, Double> preds = CUtil.makeMap();
			for (String ip : split.getTestKnown()) {
				Instance inst = aggNorm.getWekaInstance(ip);
				double dist[] = mAlgorithmsU[i].distributionForInstance(inst);
				preds.put(ip, dist[1]);
			}
			
			writeRank(getResultFile(runID, labelAnnot, i), split.getTestKnown(), split.getTestWhite(), preds);
			writeScore(getScoreFile(runID, labelAnnot, i), split.getTestWhite(), preds);
		}
	}

	private void runUU(int runID, LabelSplit split) throws Exception {
		InstanceDatabase aggNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
		String labelAnnot = "_UUlabel" + LABEL_FORMAT.format(mLabelPercentage);
		
		for (String ip : split.getTrainWhite()) {
			aggNorm.setLabel(ip, LabelSplit.LABEL_POSITIVE);
		}
		
		for (String ip : split.getTrainNonWhite()) {
			aggNorm.setLabel(ip, LabelSplit.LABEL_NEGATIVE);
		}

		Instances wekaTrain = aggNorm.getWekaTrain();
		for (int i = 0; i < mAlgorithmsU.length; i++) {
			Log.log(Level.INFO, "Building {0}...", i);
			
			mAlgorithmsU[i].buildClassifier(wekaTrain);
			
			Map<String, Double> preds = CUtil.makeMap();
			for (String ip : split.getTestAll()) {
				Instance inst = aggNorm.getWekaInstance(ip);
				double dist[] = mAlgorithmsU[i].distributionForInstance(inst);
				preds.put(ip, dist[1]);
			}
			
			writeRank(getResultFile(runID, labelAnnot, i), split.getTestAll(), split.getTestWhite(), preds);
			writeScore(getScoreFile(runID, labelAnnot, i), split.getTestWhite(), preds);
		}
	}

	private String getResultFile(int runID, String labelAnnot, int algID) {
		return Config.INSTANCE.getCurrentReportPath() + "result" + labelAnnot + "_run" + runID + "_ranker" + algID + ".csv";
	}

	private String getScoreFile(int runID, String labelAnnot, int algID) {
		return Config.INSTANCE.getCurrentReportPath() + "score" + labelAnnot + "_run" + runID + "_ranker" + algID + ".csv";
	}

	private void writeRank(String file, List<String> allIPs, List<String> whiteIPs, Map<String, Double> preds) throws IOException {
		Set<String> whiteSet = CUtil.makeSet(whiteIPs);
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		for (String ip : allIPs) {
			double pred = preds.get(ip);
			out.write(ip + ",");
			
			if (whiteSet.contains(ip)) {
				out.write("1,");
			} else {
				out.write("0,");
			}
			
			out.write(String.valueOf(pred));
			out.newLine();
		}		
		out.close();
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
		
		RankerResultList results = new RankerResultList(mAlgorithms.length, mEvaluators.length, RUNS);
		for (int i = 0; i < mAlgorithms.length; i++) {
			for (int run = 0; run < RUNS; run++) {
				results.addRun(i, run, getScoreFile(run, labelAnnot, i));
			}
		}
		
		results.write(Config.INSTANCE.getCurrentReportPath() + "summary" + labelAnnot + ".csv", mEvaluators);
	}

}
