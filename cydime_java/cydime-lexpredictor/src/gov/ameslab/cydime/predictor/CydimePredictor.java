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

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.Normalize;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.predictor.featurerank.FeatureRanker;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import weka.core.SerializationHelper;

/**
 * Cydime Predictor.
 * Defaults to building an REPTreeAVT over hierarchical features, and then stack the results over
 * remaining features to build a final REPTree.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimePredictor {
	
	private static final Logger Log = Logger.getLogger(CydimePredictor.class.getName());
	
	public static final String MODEL_FILE = ".model";
	public static final String EVALUATION_FILE = ".result";
	
	private boolean mDoDebug;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			printUsage();
			return;
		}
		
		new CydimePredictor(args[0]).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimePredictor FEATURE_DIR");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
	}
	
	public CydimePredictor() {
		mDoDebug = true;
	}
			
	public CydimePredictor(String featurePath) {
		this();
		Config.INSTANCE.setParam(featurePath);
	}
	
	public void setDoDebug(boolean v) {
		mDoDebug = v;
	}
	
	private void run() {
		try {
			run(Config.FEATURE_IP_DIR);
		} catch (Exception e) {
			Log.log(Level.SEVERE, e.toString());
			e.printStackTrace();
		}
		
		try {
			run(Config.FEATURE_ASN_DIR);
		} catch (Exception e) {
			Log.log(Level.SEVERE, e.toString());
			e.printStackTrace();
		}
	}
	
	private void run(String featureSet) throws Exception {
		Config.INSTANCE.setFeatureSet(featureSet);
		
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		InstanceDatabase baseScore = null;
		if (baseNorm.getTrainIDs().size() < Config.INSTANCE.getInt(Config.MISSION_SIM_THRESHOLD)) {
			Log.log(Level.INFO, "Available lexical mission similarity scores " + baseNorm.getTrainIDs().size() + " is under required threshold: " + Config.INSTANCE.getInt(Config.MISSION_SIM_THRESHOLD)
					+ " -- using feature ranker instead.");
			
			baseScore = predictRank(baseNorm);
		} else {
			InstanceDatabase hierarchy = InstanceDatabase.load(Config.INSTANCE.getCurrentPreprocessPath() + Config.INSTANCE.getHierarchy());
			InstanceDatabase hierarchyCV = stack("hierarchy_stack", hierarchy, ClassifierFactory.makeREPTreeAVT());
			InstanceDatabase all = InstanceDatabase.mergeFeatures(Config.INSTANCE.getStackPath(),
					hierarchyCV,
					baseNorm
					);
			all.saveIPs();
			all.write();
			all.writeReport();
			
			baseScore = learnAndPredict(ClassifierFactory.makeREPTree(), all, Config.INSTANCE.getBaseScorePath());
		}
		
		//Final result
		InstanceDatabase base = InstanceDatabase.load(Config.INSTANCE.getBasePath());
		writeFinalResult(baseNorm, baseScore, base);
	}

	private InstanceDatabase predictRank(InstanceDatabase base) throws IOException {
		FeatureRanker fr = new FeatureRanker(Config.INSTANCE.getString(Config.FEATURE_LABEL_FILE));
		fr.buildClassifier(base.getWekaInstances());
		
		ARFFWriter out = new ARFFWriter(Config.INSTANCE.getRankScorePath() + WekaPreprocess.ALL_SUFFIX, "rank_score", null,
				"rank_score numeric"
				); 
		for (String id : base.getIDs()) {
			Instance inst = base.getWekaInstance(id);
			double rank_score = fr.classifyInstance(inst);
			out.writeValues(String.valueOf(rank_score), "?");
		}
		out.close();
		
		WekaPreprocess.filterUnsuperivsed(Config.INSTANCE.getRankScorePath() + WekaPreprocess.ALL_SUFFIX,
				Normalize);
		FileUtil.copy(Config.INSTANCE.getRankScorePath() + WekaPreprocess.ALL_SUFFIX, Config.INSTANCE.getRankScorePath() + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(Config.INSTANCE.getRankScorePath(), base.getIDs());
	}

	private InstanceDatabase stack(String name, InstanceDatabase base, AbstractClassifier c) throws Exception {
		Map<String, Double> stack = CUtil.makeMap();
		
		//Prepare stack results for test (normal prediction)
		Log.log(Level.INFO, "Building {0}", c.getClass().toString());
		
		Instances train = base.getWekaTrain();
		c.buildClassifier(train);
		for (String id : base.getTestIDs()) {
			Instance inst = base.getWekaInstance(id);
			double dist = c.classifyInstance(inst);
			stack.put(id, dist);
		}
		
		//Prepare stack results for train (2-fold CV)
		List<String> cvIPs = CUtil.makeList(base.getTrainIDs());
		Collections.shuffle(cvIPs, new Random(0));
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
		for (String id : base.getIDs()) {
			values[0] = String.valueOf(stack.get(id));
			values[1] = String.valueOf(base.getLabel(id));
			out.writeValues(values);
		}
		out.close();
		
		FileUtil.copy(Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.ALL_SUFFIX, Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(Config.INSTANCE.getCurrentModelPath() + name, base.getIDs());
	}
	
	private static void stackCV(Map<String, Double> stack, InstanceDatabase base, AbstractClassifier c, List<String> trainIPs, List<String> testIPs) throws Exception {
		Instances train = new Instances(base.getWekaTrain(), 0);
		for (String id : trainIPs) {
			Instance inst = base.getWekaInstance(id);
			inst.setDataset(train);
			train.add(inst);
		}
		
		c.buildClassifier(train);
		
		for (String id : testIPs) {
			Instance inst = base.getWekaInstance(id);
			double dist = c.classifyInstance(inst);
			stack.put(id, dist);
		}
	}
	
	private void writeFinalResult(InstanceDatabase baseNorm, InstanceDatabase baseScore, InstanceDatabase base) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getFinalResultPath()));
		out.write("ID,lexical_norm,lexical_predicted,byte,byte_norm,mission_norm,exfiltration_norm");
		out.newLine();
		
		for (String id : baseNorm.getIDs()) {
			double lexical = 0.0;
			boolean isLexicalPredicted = false;
			if (baseNorm.hasLabel(id)) {
				lexical = baseNorm.getLabel(id);
			} else {
				isLexicalPredicted = true;
				lexical = baseScore.getWekaInstance(id).value(0);
			}		
			
			double bytes = base.getWekaReportInstance(id).value(3);
			double bytesNorm = baseNorm.getWekaInstance(id).value(3);
			double mission = getMission(lexical, bytesNorm);
			double exfiltration = getExfiltration(isLexicalPredicted ? 0.0 : lexical, bytesNorm);
			
			out.write(id + ",");
			out.write(String.valueOf(lexical));
			out.write(",");
			out.write(isLexicalPredicted ? "1" : "0");
			out.write(",");
			out.write(String.valueOf(bytes));
			out.write(",");
			out.write(String.valueOf(bytesNorm));
			out.write(",");
			out.write(String.valueOf(mission));
			out.write(",");
			out.write(String.valueOf(exfiltration));
			out.newLine();
		}
		out.close();
	}

	private double getMission(double lexical, double bytesNorm) {
		return Math.sqrt(lexical * lexical + bytesNorm * bytesNorm);
	}

	private double getExfiltration(double lexical, double bytesNorm) {
		lexical = 1.0 - lexical;
		return Math.sqrt(lexical * lexical + bytesNorm * bytesNorm);
	}
	
	private InstanceDatabase learnAndPredict(AbstractClassifier c, InstanceDatabase input, String output) throws Exception {
		Log.log(Level.INFO, "Learning {0} ...", output);
		
		Instances train = input.getWekaTrain();
		Instances test = input.getWekaTest();
		
		c.buildClassifier(train);
		
		if (mDoDebug) {
			SerializationHelper.write(output + MODEL_FILE, c);
			WekaPreprocess.save(train, input.getTrainPath());
			WekaPreprocess.save(test, input.getTestPath());
			writeEvaluation(train, c, output + EVALUATION_FILE);
		}
		
		Map<String, Double> prediction = predict(c, input);
		return writeOutput(prediction, input, output);
	}
	
	private void writeEvaluation(Instances train, AbstractClassifier c, String file) throws Exception {
		Evaluation eval = new Evaluation(train);
		eval.crossValidateModel(c, train, 10, new Random(0));
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write(eval.toSummaryString());
		out.close();
	}

	private Map<String, Double> predict(AbstractClassifier c, InstanceDatabase input) throws Exception {
		Map<String, Double> pred = CUtil.makeMap();
		for (String id : input.getTestIDs()) {
			Instance inst = input.getWekaInstance(id);
			double dist = c.classifyInstance(inst);
			pred.put(id, dist);
		}
		return pred;
	}

	private InstanceDatabase writeOutput(Map<String, Double> prediction, InstanceDatabase input, String file) throws IOException {
		File path = new File(file);
		String name = path.getName();
		ARFFWriter out = new ARFFWriter(file + WekaPreprocess.ALL_SUFFIX, name, null, name + " numeric"); 
		for (String id : input.getTestIDs()) {
			double pred = prediction.get(id);
			out.writeValues(String.valueOf(pred), "?");
		}
		out.close();
		
		FileUtil.copy(file + WekaPreprocess.ALL_SUFFIX, file + WekaPreprocess.REPORT_SUFFIX);
		InstanceDatabase output = new InstanceDatabase(file, input.getTestIDs());
		output.saveIPs();
		return output;
	}

}
