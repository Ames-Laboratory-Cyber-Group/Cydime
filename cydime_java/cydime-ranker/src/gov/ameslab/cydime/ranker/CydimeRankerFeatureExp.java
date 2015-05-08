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
import gov.ameslab.cydime.ranker.evaluate.FeatureWrapper;
import gov.ameslab.cydime.ranker.evaluate.NDCG;
import gov.ameslab.cydime.ranker.evaluate.RankEvaluator;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.AbstractClassifier;

/**
 * Cydime Predictor experimenter using a set of representative predictors.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeRankerFeatureExp {

	private static final Logger Log = Logger.getLogger(CydimeRankerFeatureExp.class.getName());

	public static final int RUNS = 10;
	public static final double TRAIN_PERCENT = 100.0 * 2 / 3;
	
	private AbstractClassifier[] mAlgorithms = new AbstractClassifier[] {
			RankerFactory.makeNaiveBayes(),
			RankerFactory.makeLogistic(),
			RankerFactory.makeREPTree(),
			RankerFactory.makeLibLINEAR(),
			RankerFactory.makeLibSVM(),
//			RankerFactory.makeResampleEnsemble(RankerFactory.makeLibSVM(10), 1.0, new Random(1)),
//			RankerFactory.makeResampleEnsemble(RankerFactory.makeLibSVMNu(10), 1.0, new Random(1)),
	};
	
	private RankEvaluator[] mEvaluators = new RankEvaluator[] {
			new AveragePrecision(),
			new NDCG(),
	};
	
	private List<String> mAttributeList;
	private LabelSplit[] mSplits;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			printUsage();
			return;
		}

		new CydimeRankerFeatureExp(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimeRankerFeatureExp FEATURE_DIR");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
	}

	public CydimeRankerFeatureExp(String[] args) {
		Config.INSTANCE.setParam(args[0]);
		Config.INSTANCE.setFeatureSet(Config.FEATURE_IP_DIR);
	}

	private void run() throws Exception {
		makeSplits();
		
		for (int e = 0; e < mEvaluators.length; e++) {
			Log.log(Level.INFO, "Running evaluator {0}...", e);
			
			for (int i = 0; i < mAlgorithms.length; i++) {
				Log.log(Level.INFO, "Running ranker {0}...", i);
				
				BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getCurrentReportPath() + "feature_wrapper_e" + e + "a" + i + ".csv"));
							
				FeatureWrapper wrap = new FeatureWrapper(mAlgorithms[i], mEvaluators[e], mSplits, mAttributeList.size() - 1);
//				while (wrap.findNext()) {
				for (int j = 0; j < 33; j++) {
					wrap.findNext();
					int feature = wrap.getLastFeature();
					double eval = wrap.getLastEval();
					out.write(mAttributeList.get(feature) + "," + eval);
					out.newLine();
					
					if (wrap.getFeatureSize() >= mAttributeList.size() - 1) break;
				}
				
				out.close();
			}
		}
	}

	private void makeSplits() throws IOException {
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		mAttributeList = baseNorm.getAttributeList();
		
		List<String> ips = CUtil.makeList(baseNorm.getIDs());
		
		ListDatabase whiteDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_WHITE_FILE));
		LabelSample whiteLabel = new LabelSample(whiteDB.getList(ips));
		
		ListDatabase blackDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_BLACK_FILE));
		LabelSample blackLabel = new LabelSample(blackDB.getList(ips));
		
		mSplits = new LabelSplit[RUNS];
		for (int run = 0; run < RUNS; run++) {
			List<String> whiteSample = whiteLabel.getNextSample();
			List<String> blackSample = blackLabel.getNextSample();
			mSplits[run] = new LabelSplit(ips, whiteSample, blackSample, TRAIN_PERCENT, 100.0, new Random(run));
		}
	}

}
