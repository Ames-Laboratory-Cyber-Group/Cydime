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

public class CydimePredictor {
	
	private static final Logger Log = Logger.getLogger(CydimePredictor.class.getName());
	
	public static final String MODEL_FILE = ".model";
	public static final String EVALUATION_FILE = ".result";
	
	private boolean mDoDebug;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			printUsage();
			return;
		}
		
		new CydimePredictor(args[0]).run();
	}

	private static void printUsage() {
		System.out.println("java -jar ORCPredictor FEATURE_DIR");
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
	
	private void run() throws Exception {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		InstanceDatabase hierarchy = InstanceDatabase.load(Config.INSTANCE.getCurrentPreprocessPath() + Config.INSTANCE.getHierarchy());
		InstanceDatabase hierarchyCV = stack("hierarchy_stack", hierarchy, ClassifierFactory.makeREPTreeAVT());
		InstanceDatabase all = InstanceDatabase.mergeFeatures(Config.INSTANCE.getStackPath(),
				hierarchyCV,
				baseNorm
				);
		all.saveIPs();
		all.write();
		all.writeReport();
		
		InstanceDatabase baseScore = learnAndPredict(ClassifierFactory.makeREPTree(), all, Config.INSTANCE.getBaseScorePath());
		
		//Report
		InstanceDatabase baseNormCV = stack("baseNorm_stack", baseNorm, ClassifierFactory.makeREPTree());
		InstanceDatabase predicted = InstanceDatabase.mergeFeatures(Config.INSTANCE.getAllPredictedPath(),
				hierarchyCV,
				baseNormCV);
		predicted.writeReport();
		
		//Final result
		writeFinalResult(baseNorm, baseScore);
	}

	private InstanceDatabase stack(String name, InstanceDatabase base, AbstractClassifier c) throws Exception {
		Map<String, Double> stack = CUtil.makeMap();
		
		//Prepare stack results for test (normal prediction)
		Log.log(Level.INFO, "Building {0}", c.getClass().toString());
		
		Instances train = base.getWekaTrain();
		c.buildClassifier(train);
		for (String ip : base.getTestIPs()) {
			Instance inst = base.getWekaInstance(ip);
			double dist = c.classifyInstance(inst);
			stack.put(ip, dist);
		}
		
		//Prepare stack results for train (2-fold CV)
		List<String> cvIPs = CUtil.makeList(base.getTrainIPs());
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
		for (String ip : base.getIPs()) {
			values[0] = String.valueOf(stack.get(ip));
			values[1] = String.valueOf(base.getLabel(ip));
			out.writeValues(values);
		}
		out.close();
		
		FileUtil.copy(Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.ALL_SUFFIX, Config.INSTANCE.getCurrentModelPath() + name + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(Config.INSTANCE.getCurrentModelPath() + name, base.getIPs());
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
	
	private void writeFinalResult(InstanceDatabase all, InstanceDatabase test) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getFinalResultPath()));
		out.write("IP,score");
		out.newLine();
		
		for (String ip : all.getIPs()) {
			out.write(ip + ",");
			
			if (all.hasLabel(ip)) {
				out.write(String.valueOf(all.getLabel(ip)));
			} else {
				Instance inst = test.getWekaInstance(ip);
				out.write(String.valueOf(inst.value(0)));
			}
			out.newLine();
		}
		out.close();
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
		for (String ip : input.getTestIPs()) {
			Instance inst = input.getWekaInstance(ip);
			double dist = c.classifyInstance(inst);
			pred.put(ip, dist);
		}
		return pred;
	}

	private InstanceDatabase writeOutput(Map<String, Double> prediction, InstanceDatabase input, String file) throws IOException {
		File path = new File(file);
		String name = path.getName();
		ARFFWriter out = new ARFFWriter(file + WekaPreprocess.ALL_SUFFIX, name, null, name + " numeric"); 
		for (String ip : input.getTestIPs()) {
			double pred = prediction.get(ip);
			out.writeValues(String.valueOf(pred), "?");
		}
		out.close();
		
		FileUtil.copy(file + WekaPreprocess.ALL_SUFFIX, file + WekaPreprocess.REPORT_SUFFIX);
		InstanceDatabase output = new InstanceDatabase(file, input.getTestIPs());
		output.saveIPs();
		return output;
	}

}
