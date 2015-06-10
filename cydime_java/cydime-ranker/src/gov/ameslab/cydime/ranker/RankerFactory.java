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

import gov.ameslab.cydime.util.CUtil;

import java.util.List;
import java.util.Random;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.KernelLogisticRegression;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.OneClassClassifier;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;
import weka.core.SelectedTag;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class RankerFactory {

	public static AbstractClassifier makeRandomRanker() {
		return new RandomRanker();
	}
	
	public static AbstractClassifier makeFeatureProjector(int index) {
		return new FeatureProjector(index);
	}
	
	public static AbstractClassifier makeLogistic() {
		Logistic c = new Logistic();
		try {
			c.setOptions("-R 1.0E-8 -M -1".split(" "));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
	
	public static AbstractClassifier makeKernelLogisticPoly() {
		KernelLogisticRegression c = new KernelLogisticRegression();
		PolyKernel k = new PolyKernel();
		k.setExponent(2.0);
		c.setKernel(k);
		return c;
	}
	
	public static AbstractClassifier makeKernelLogisticRBF() {
		KernelLogisticRegression c = new KernelLogisticRegression();
		c.setKernel(new RBFKernel());
		return c;
	}

	public static AbstractClassifier makeNaiveBayes() {
		NaiveBayes c = new NaiveBayes();
		return c;
	}

	public static AbstractClassifier makeAdaBoostM1() {
		AdaBoostM1 c = new AdaBoostM1();
		return c;
	}
	
	public static AbstractClassifier makeRandomForest() {
		RandomForest c = new RandomForest();
		return c;
	}
	
//	public static AbstractClassifier makeLibLINEAR() {
//		LibLINEAR c = new LibLINEAR();
//		c.setSVMType(new SelectedTag(0, LibLINEAR.TAGS_SVMTYPE));
//		c.setProbabilityEstimates(true);
//		return c;
//	}
	
	public static AbstractClassifier makeLibSVM() {
		LibSVM c = new LibSVM();
		c.setProbabilityEstimates(true);
		return c;
	}
	
	public static AbstractClassifier makeLibSVMNu() {
		LibSVM c = new LibSVM();
		c.setSVMType(new SelectedTag(1, LibLINEAR.TAGS_SVMTYPE));
		c.setProbabilityEstimates(true);
		return c;
	}

	public static AbstractClassifier makeREPTree() {
		REPTree c = new REPTree();
		return c;
	}

	public static AbstractClassifier makeOneClass() {
		OneClassClassifier c = new OneClassClassifier();
		Bagging b = new Bagging();
		b.setClassifier(makeLogistic());
		c.setClassifier(b);
		c.setTargetClassLabel("1");
		return c;
	}

	public static List<AbstractClassifier> makeLogistic(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeLogistic());
		}
		return cs;
	}
	
	public static List<AbstractClassifier> makeKernelLogisticPoly(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeKernelLogisticPoly());
		}
		return cs;
	}
	
	public static List<AbstractClassifier> makeKernelLogisticRBF(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeKernelLogisticRBF());
		}
		return cs;
	}
		
	public static List<AbstractClassifier> makeNaiveBayes(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeNaiveBayes());
		}
		return cs;
	}
	
	public static List<AbstractClassifier> makeAdaBoostM1(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeAdaBoostM1());
		}
		return cs;
	}
	
	public static List<AbstractClassifier> makeRandomForest(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeRandomForest());
		}
		return cs;
	}
	
//	public static List<AbstractClassifier> makeLibLINEAR(int n) {
//		List<AbstractClassifier> cs = CUtil.makeList();
//		for (int i = 0; i < n; i++) {
//			cs.add(makeLibLINEAR());
//		}
//		return cs;
//	}
	
	public static List<AbstractClassifier> makeLibSVM(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeLibSVM());
		}
		return cs;
	}
	
	public static List<AbstractClassifier> makeLibSVMNu(int n) {
		List<AbstractClassifier> cs = CUtil.makeList();
		for (int i = 0; i < n; i++) {
			cs.add(makeLibSVMNu());
		}
		return cs;
	}
		
	public static AbstractClassifier makeResampleEnsemble(List<AbstractClassifier> bases, double negOverPos) {
		return makeResampleEnsemble(bases, negOverPos, new Random(1));
	}
	
	public static AbstractClassifier makeResampleEnsemble(List<AbstractClassifier> bases, double negOverPos, Random seed) {
		return new ResampleEnsemble(bases, negOverPos, seed);
	}

}
