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

import gov.ameslab.classifier.REPTreeAVT;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.trees.REPTree;
import weka.core.SelectedTag;

public class ClassifierFactory {

	public static AbstractClassifier makeLogistic() throws Exception {
		Logistic c = new Logistic();
		c.setOptions("-R 1.0E-8 -M -1".split(" "));
		return c;
	}

	public static AbstractClassifier makeNaiveBayes() {
		NaiveBayes c = new NaiveBayes();
		return c;
	}

	public static AbstractClassifier makeLibLINEAR() {
		LibLINEAR c = new LibLINEAR();
		c.setSVMType(new SelectedTag(0, LibLINEAR.TAGS_SVMTYPE));
		c.setProbabilityEstimates(true);
		return c;
	}
	
	
	public static AbstractClassifier makeLinearRegression() {
		LinearRegression c = new LinearRegression();
		return c;
	}
	
	public static AbstractClassifier makeGaussianProcesses() {
		GaussianProcesses c = new GaussianProcesses();
		return c;
	}
	
	public static AbstractClassifier makeEpsilonSVR() {
		LibSVM c = new LibSVM();
		c.setSVMType(new SelectedTag(LibSVM.SVMTYPE_EPSILON_SVR, LibSVM.TAGS_SVMTYPE));
		c.setProbabilityEstimates(true);
		return c;
	}
	
	public static AbstractClassifier makeNuSVR() {
		LibSVM c = new LibSVM();
		c.setSVMType(new SelectedTag(LibSVM.SVMTYPE_NU_SVR, LibSVM.TAGS_SVMTYPE));
		c.setProbabilityEstimates(true);
		return c;
	}
	
	public static AbstractClassifier makeAdditiveRegression() {
		AdditiveRegression c = new AdditiveRegression();
		return c;
	}
	
	public static AbstractClassifier makeREPTree() {
		REPTree c = new REPTree();
		return c;
	}
	
	public static AbstractClassifier makeSMOreg() {
		SMOreg c = new SMOreg();
		return c;
	}
	
	public static AbstractClassifier makeREPTreeAVT() {
		REPTreeAVT c = new REPTreeAVT();
		return c;
	}
	
}
