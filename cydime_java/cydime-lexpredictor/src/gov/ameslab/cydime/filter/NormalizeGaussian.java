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

package gov.ameslab.cydime.filter;

import java.io.Serializable;
import java.util.Arrays;

import umontreal.iro.lecuyer.probdist.NormalDist;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.matrix.LinearRegression;
import weka.core.matrix.Matrix;
import weka.filters.SimpleBatchFilter;

public class NormalizeGaussian extends SimpleBatchFilter {

	private static final long serialVersionUID = -3392794845073011810L;

	static class Param implements Serializable {

		private static final long serialVersionUID = 3925174757303230094L;
		
		double XMin;
		double XMax;
		double[] Weights;
		
		public double normalize(double x) {
			if (x < XMin) x = XMin;
			else if (x > XMax) x = XMax;
			return Weights[0] + Weights[1] * x + Weights[2] * Math.log(1.0 + x);
		}
	}
	
	private Param[] mParams;
	
	@Override
	protected Instances determineOutputFormat(Instances data) throws Exception {
		return data;
	}

	@Override
	public String globalInfo() {
		return "Normalize with Gaussian";
	}

	/**
	 * Returns default capabilities of the classifier.
	 *
	 * @return the capabilities of this filter
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.NOMINAL_ATTRIBUTES);

		// class
		result.enableAllClasses();
		result.enable(Capability.MISSING_CLASS_VALUES);
	    result.enable(Capability.NO_CLASS);
	    
		// instances
		result.setMinimumNumberInstances(0);

		return result;
	}
	
	@Override
	protected Instances process(Instances data) throws Exception {
		if (!isFirstBatchDone()) {
			Instances output = determineOutputFormat(data);
			setOutputFormat(output);
			computeParams(data);
		}
		
		Instances outputFormat = getOutputFormat();
		Instances result = new Instances(outputFormat, 0);
		for (int i = 0; i < data.numInstances(); i++) {
			Instance dataI = data.instance(i);
			Instance newDataI = new DenseInstance(outputFormat.numAttributes());
			newDataI.setDataset(result);
			int newJ = 0;
			for (int j = 0; j < dataI.numAttributes(); j++) {
				Attribute a = dataI.attribute(j);
				if (a.isNumeric()) {
					double value = dataI.value(j);
					double newValue = mParams[j].normalize(value);
					newDataI.setValue(newJ++, newValue);
				} else {
					newDataI.setValue(newJ++, dataI.stringValue(j));
				}
			}
			result.add(newDataI);
		}
		
		result.setClassIndex(data.classIndex());
		
		return result;
	}

	private void computeParams(Instances data) {
		mParams = new Param[data.numAttributes()];
		
		for (int j = 0; j < data.numAttributes(); j++) {
			if (!data.attribute(j).isNumeric()) continue;
			
			double[] values = new double[data.numInstances()];
			for (int i = 0; i < data.numInstances(); i++) {
				values[i] = data.instance(i).value(j);
			}
			mParams[j] = learnNormalizeParams(values);
		}
	}

	private Param learnNormalizeParams(double[] allValues) {
		Param p = new Param();
		
		Arrays.sort(allValues);
		int iMin = (int)Math.ceil(0.01 * allValues.length + 0.5) - 1;
		int iMax = (int)(0.99 * allValues.length + 0.5) - 1;
		p.XMin = allValues[iMin];
		p.XMax = allValues[iMax];
		
		double[][] R = new double[iMax - iMin + 1][3];
		double[][] Y = new double[iMax - iMin + 1][1];
		for (int i = iMin; i <= iMax; i++) {
			R[i - iMin][0] = 1;
			R[i - iMin][1] = allValues[i];
			R[i - iMin][2] = Math.log(allValues[i] + 1);
			Y[i - iMin][0] = NormalDist.inverseF01((i + 0.5) / allValues.length) / 3;
		}
		
		try {
			Matrix matR = new Matrix(R);
			Matrix matY = new Matrix(Y);
			LinearRegression lr = new LinearRegression(matR, matY, 0.0);
			p.Weights = lr.getCoefficients();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return p;
	}

	public static void main(String[] args) {
		
	}
	
}
