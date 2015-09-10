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
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Cydime Predictor experimenter using a set of representative predictors.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeRanker {

	private static final Logger Log = Logger.getLogger(CydimeRanker.class.getName());

	private static final int RUNS = 10;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			printUsage();
			return;
		}

		new CydimeRanker(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimeRanker FEATURE_DIR");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
	}

	public CydimeRanker(String[] args) {
		Config.INSTANCE.setParam(args[0]);
		Config.INSTANCE.setFeatureSet(Config.FEATURE_IP_DIR);
	}

	private void run() throws Exception {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		
		List<String> ips = CUtil.makeList(baseNorm.getIDs());
		
		ListDatabase whiteDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_WHITE_FILE));
		LabelSample whiteLabel = new LabelSample(whiteDB.getList(ips));
		
		ListDatabase blackDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_BLACK_FILE));
		LabelSample blackLabel = new LabelSample(blackDB.getList(ips));
		
		Map<String, Double> preds = CUtil.makeMap();
		for (int run = 0; run < RUNS; run++) {
			List<String> whiteSample = whiteLabel.getNextSample();
			List<String> blackSample = blackLabel.getNextSample();
			LabelSplit split = new LabelSplit(ips, whiteSample, blackSample, 100.0, 100.0, new Random(run));
			run(run, split, preds);
		}
		
		CUtil.divide(preds, RUNS);
		
		writeFinalResult(baseNorm, preds);
	}

	private void run(int runID, LabelSplit split, Map<String, Double> preds) throws Exception {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		
		for (String ip : split.getTrainWhite()) {
			baseNorm.setLabel(ip, LabelSplit.LABEL_POSITIVE);
		}
		
		for (String ip : split.getTrainNonWhite()) {
			baseNorm.setLabel(ip, LabelSplit.LABEL_NEGATIVE);
		}

		Instances wekaTrain = baseNorm.getWekaTrain();
		AbstractClassifier c = RankerFactory.makeAdaBoostM1();
		c.buildClassifier(wekaTrain);
		
		FileUtil.writeFile(Config.INSTANCE.getModelPath() + runID + ".txt", c.toString());
		SerializationHelper.write(Config.INSTANCE.getModelPath() + runID + ".model", c);
		
		Map<String, Double> pred = CUtil.makeMap();
		for (String ip : split.getAll()) {
			Instance inst = baseNorm.getWekaInstance(ip);
			double dist[] = c.distributionForInstance(inst);
			pred.put(ip, dist[1]);
		}
		
		CUtil.add(preds, pred);
	}

	private void writeFinalResult(InstanceDatabase baseNorm, Map<String, Double> preds) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getFinalResultPath()));
		out.write("ID,score");
		out.newLine();
		
		for (String id : baseNorm.getIDs()) {
			out.write(id + ",");
			out.write(String.valueOf(preds.get(id)));
			out.newLine();
		}
		out.close();
	}

}
