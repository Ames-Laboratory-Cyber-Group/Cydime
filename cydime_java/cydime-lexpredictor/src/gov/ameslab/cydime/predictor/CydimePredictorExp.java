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

package gov.ameslab.cydime.predictor;

import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Cydime Predictor experimenter using a set of representative predictors.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimePredictorExp {

	private static final Logger Log = Logger.getLogger(CydimePredictorExp.class.getName());

	public static final String MODEL_FILE = ".model";
	public static final String EVALUATION_FILE = ".result";

	private enum Algorithm { STRENGTH, HIERARCHY, HYBRID }

	private Algorithm mAlgorithmSet;
	private AbstractClassifier[] mAlgorithms = new AbstractClassifier[] {
			ClassifierFactory.makeLinearRegression(),
			ClassifierFactory.makeREPTree(),
			ClassifierFactory.makeAdditiveRegression(),
			ClassifierFactory.makeEpsilonSVR(),
			ClassifierFactory.makeNuSVR(),
			ClassifierFactory.makeGaussianProcesses(),
	};
	private AbstractClassifier mHierarchyAlgorithm = ClassifierFactory.makeREPTreeAVT();
	
	private int mAlgorithm;
	private double mLabelPercentage;
	private int mSeed;

	
	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			printUsage();
			return;
		}

		new CydimePredictorExp(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimePredictorExp FEATURE_DIR ALGO_SET ALGO LABEL_PERCENT SEED");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
		System.out.println("    ALGO_SET: [strength|hierarchy|hybrid]");
		System.out.println("    ALGO: [0|1|2|3|4]");
		System.out.println("          0 - LinearRegression");
		System.out.println("          1 - REPTree");
		System.out.println("          2 - AdditiveRegression");
		System.out.println("          3 - EpsilonSVR");
		System.out.println("          4 - NuSVR");
		System.out.println("          5 - GaussianProcesses");
		System.out.println("    LABEL_PERCENT: [1,99] percentage of labeled instances");
		System.out.println("    SEED: Integer of random seed");
	}

	public CydimePredictorExp(String[] args) {
		Config.INSTANCE.setParam(args[0]);
		Config.INSTANCE.setFeatureSet(Config.FEATURE_IP_DIR);

		if (args[1].equalsIgnoreCase("strength")) {
			mAlgorithmSet = Algorithm.STRENGTH;
		} else if (args[1].equalsIgnoreCase("hierarchy")) {
			mAlgorithmSet = Algorithm.HIERARCHY;
		} else if (args[1].equalsIgnoreCase("hybrid")) {
			mAlgorithmSet = Algorithm.HYBRID;
		}

		mAlgorithm = Integer.parseInt(args[2]);
		mLabelPercentage = Double.parseDouble(args[3]);
		mSeed = Integer.parseInt(args[4]);
	}

	private void run() throws Exception {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		InstanceDatabase hierarchy = InstanceDatabase.load(Config.INSTANCE.getCurrentPreprocessPath() + Config.INSTANCE.getHierarchy());
		
		List<String> ips = CUtil.makeList(baseNorm.getIDs());
		Collections.shuffle(ips, new Random((long) (mSeed * 100 * mLabelPercentage)));

		int cutoff = (int) (ips.size() * mLabelPercentage / 100.0);
		List<String> trainIPs = ips.subList(0, cutoff);
		List<String> testIPs = ips.subList(cutoff, ips.size());

		Log.log(Level.INFO, "Prepared train: " + trainIPs.size() + " test: " + testIPs.size());
		
		//record test labels
		Map<String, Double> labels = CUtil.makeMap();
		for (String ip : testIPs) {
			double label = baseNorm.getLabel(ip);
			labels.put(ip, label);
		}
		
		//set test class to missing
		for (String ip : testIPs) {
			baseNorm.setLabel(ip, null);
			hierarchy.setLabel(ip, null);
		}

		//run algorithm, get predictions
		Map<String, Double> preds = null;		
		switch (mAlgorithmSet) {
		case STRENGTH:
			preds = runStrength(baseNorm);
			break;

		case HIERARCHY:
			preds = runHierarchy(hierarchy);			
			break;

		case HYBRID:
			preds = runHybrid(baseNorm, hierarchy);
			break;
			
		}
		
		writeEvaluation(baseNorm, labels, preds);
	}

	private Map<String, Double> runStrength(InstanceDatabase baseNorm) throws Exception {
		Instances train = baseNorm.getWekaTrain();
		Log.log(Level.INFO, "Building {0}", mAlgorithms[mAlgorithm].getClass().toString());
		mAlgorithms[mAlgorithm].buildClassifier(train);
		Log.log(Level.INFO, "Finished {0}", mAlgorithms[mAlgorithm].toString());
		
		Map<String, Double> pred = CUtil.makeMap();
		for (String ip : baseNorm.getTestIDs()) {
			Instance inst = baseNorm.getWekaInstance(ip);
			double dist = mAlgorithms[mAlgorithm].classifyInstance(inst);
			pred.put(ip, dist);
		}
		
		return pred;
	}

	private Map<String, Double> runHierarchy(InstanceDatabase hierarchy) throws Exception {
		Instances train = hierarchy.getWekaTrain();
		
		Log.log(Level.INFO, "Building {0}", mHierarchyAlgorithm.getClass().toString());
		mHierarchyAlgorithm.buildClassifier(train);
		Log.log(Level.INFO, "Finished {0}", mHierarchyAlgorithm.toString());
		
		Map<String, Double> pred = CUtil.makeMap();
		for (String ip : hierarchy.getTestIDs()) {
			Instance inst = hierarchy.getWekaInstance(ip);
			double dist = mHierarchyAlgorithm.classifyInstance(inst);
			pred.put(ip, dist);
		}
		
		return pred;
	}

	private Map<String, Double> runHybrid(InstanceDatabase baseNorm, InstanceDatabase hierarchy) throws Exception {
		InstanceDatabase hierarchyCV = stack("hierarchy_stack", hierarchy, mHierarchyAlgorithm);
		
		InstanceDatabase all = InstanceDatabase.mergeFeatures(Config.INSTANCE.getStackPath(),
				hierarchyCV,
				baseNorm
				);
		all.saveIPs();
		all.write();
		all.writeReport();
		
		Instances train = all.getWekaTrain();
		Log.log(Level.INFO, "Building {0}", mAlgorithms[mAlgorithm].getClass().toString());
		mAlgorithms[mAlgorithm].buildClassifier(train);
		Log.log(Level.INFO, "Finished {0}", mAlgorithms[mAlgorithm].toString());
		
		Map<String, Double> pred = CUtil.makeMap();
		for (String ip : baseNorm.getTestIDs()) {
			Instance inst = all.getWekaInstance(ip);
			double dist = mAlgorithms[mAlgorithm].classifyInstance(inst);
			pred.put(ip, dist);
		}
		
		return pred;
	}

	private InstanceDatabase stack(String name, InstanceDatabase base, AbstractClassifier c) throws Exception {
		Map<String, Double> stack = CUtil.makeMap();
		
		//Prepare stack results for test (normal prediction)
		Log.log(Level.INFO, "Building {0}", c.getClass().toString());
		
		Instances train = base.getWekaTrain();
		c.buildClassifier(train);
		for (String ip : base.getTestIDs()) {
			Instance inst = base.getWekaInstance(ip);
			double dist = c.classifyInstance(inst);
			stack.put(ip, dist);
		}
		
		//Prepare stack results for train (2-fold CV)
		List<String> cvIPs = CUtil.makeList(base.getTrainIDs());
		Collections.shuffle(cvIPs, new Random(mSeed));
		int cutoff = cvIPs.size() / 2;
		List<String> cvFold1IPs = cvIPs.subList(0, cutoff);
		List<String> cvFold2IPs = cvIPs.subList(cutoff, cvIPs.size());
		
		stackCV(stack, base, c, cvFold1IPs, cvFold2IPs);
		stackCV(stack, base, c, cvFold2IPs, cvFold1IPs);
				
		//Write stack results to ARFF file
		ARFFWriter out = new ARFFWriter(Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.ALL_SUFFIX, name, null,
				name + " numeric"
				); 
		
		String[] values = new String[2];
		for (String ip : base.getIDs()) {
			values[0] = String.valueOf(stack.get(ip));
			values[1] = String.valueOf(base.getLabel(ip));
			out.writeValues(values);
		}
		out.close();
		
		FileUtil.copy(Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.ALL_SUFFIX, Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(Config.INSTANCE.getCurrentModelPath() + name, base.getIDs());
	}

	private static void stackCV(Map<String, Double> stack, InstanceDatabase base, AbstractClassifier c, List<String> trainIPs, List<String> testIPs) throws Exception {
		Instances train = new Instances(base.getWekaTrain(), 0);
		for (String ip : trainIPs) {
			Instance inst = base.getWekaInstance(ip);
			inst.setDataset(train);
			train.add(inst);
		}
		
		c.buildClassifier(train);
		
		for (String ip : testIPs) {
			Instance inst = base.getWekaInstance(ip);
			double dist = c.classifyInstance(inst);
			stack.put(ip, dist);
		}
	}

	private void writeEvaluation(InstanceDatabase base, Map<String, Double> labels, Map<String, Double> preds) throws Exception {
		Instances train = base.getWekaTrain();
		Evaluation eval = new Evaluation(train);
		
		for (String ip : labels.keySet()) {
			double label = labels.get(ip);
			double pred = preds.get(ip);
			base.setLabel(ip, label);
			Instance inst = base.getWekaInstance(ip);
			eval.evaluateModelOnce(pred, inst);
		}
		
		DecimalFormat labelFormat = new DecimalFormat("0.0000");
		
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getCurrentReportPath() + "result" + mAlgorithmSet + "-" + mAlgorithm + "_" + labelFormat.format(mLabelPercentage) + "_" + mSeed + ".txt"));
		out.write(eval.toSummaryString());
		out.close();
		
		BufferedWriter outIP = new BufferedWriter(new FileWriter(Config.INSTANCE.getCurrentReportPath() + "result" + mAlgorithmSet + "-" + mAlgorithm + "_" + labelFormat.format(mLabelPercentage) + "_" + mSeed + ".csv"));
		for (String ip : labels.keySet()) {
			double label = labels.get(ip);
			double pred = preds.get(ip);
			outIP.write(ip + "," + label + "," + pred);
			outIP.newLine();
		}		
		outIP.close();
	}

}
