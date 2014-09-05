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

import gov.ameslab.cydime.util.CUtil;

import java.util.Map;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.SimpleBatchFilter;

public class ShiftPositive extends SimpleBatchFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 707831870765519785L;

	private Map<Integer, Double> mMinima;
	
	@Override
	protected Instances determineOutputFormat(Instances data) throws Exception {
		mMinima = CUtil.makeMap();
		
		for (int i = 0; i < data.numInstances(); i++) {
			Instance dataI = data.instance(i);
			
			for (int j = 0; j < dataI.numAttributes(); j++) {
				if (!dataI.isMissing(j)) {
					Attribute a = dataI.attribute(j);
					if (a.isNumeric()) {
						double value = dataI.value(j);
						updateMinima(j, value);
					}
				}
			}
		}
		
		return data;
	}

	private void updateMinima(int i, double value) {
		Double min = mMinima.get(i);
		if (min == null || value < min) {
			min = value;
			mMinima.put(i, min);
		}
	}

	@Override
	public String globalInfo() {
		return "Shift all numeric values such that their minimum is 0.";
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
		result.enable(Capability.MISSING_VALUES);
		
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
		}
		
		Instances outputFormat = getOutputFormat();
		Instances result = new Instances(outputFormat, 0);
		for (int i = 0; i < data.numInstances(); i++) {
			Instance dataI = data.instance(i);
			Instance newDataI = new DenseInstance(outputFormat.numAttributes());
			newDataI.setWeight(dataI.weight());
			newDataI.setDataset(result);
			int newJ = 0;
			for (int j = 0; j < dataI.numAttributes(); j++) {
				if (dataI.isMissing(j)) {
					newDataI.setMissing(newJ++);
				} else {
					Attribute a = dataI.attribute(j);
					if (a.isNumeric()) {
						double value = dataI.value(j);
						Double min = mMinima.get(j);
						if (min == null) min = 0.0;
						double newValue = value - min;
						newDataI.setValue(newJ++, newValue);
					} else {
						newDataI.setValue(newJ++, dataI.stringValue(j));
					}
				}
			}
			result.add(newDataI);
		}
		result.setClassIndex(data.classIndex());
		
		return result;
	}
	
}
