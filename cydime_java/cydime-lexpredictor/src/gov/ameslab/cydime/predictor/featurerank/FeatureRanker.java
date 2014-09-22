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

package gov.ameslab.cydime.predictor.featurerank;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class FeatureRanker implements Serializable {
	
	private static final Logger Log = Logger.getLogger(FeatureRanker.class.getName());
	
	private static final long serialVersionUID = -4358112252981457485L;

	private String mLabelFile;
	
	private List<FeatureClassifier> mFeatures;
	
	public FeatureRanker(String labelFile) {
		mLabelFile = labelFile;
	}

	public void buildClassifier(Instances instances) {
		try {
			loadFeatureLabel(instances);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		for (FeatureClassifier c : mFeatures) {
			for (int i = 0; i < instances.numInstances(); i++) {
				Instance inst = instances.instance(i);
				c.scan(inst);
			}
		}
	}

	private void loadFeatureLabel(Instances instances) throws IOException {
		mFeatures = CUtil.makeList();
		BufferedReader in = new BufferedReader(new FileReader(mLabelFile));
		String line = null;		
		while ((line = in.readLine()) != null) {
			//total_records 0.1 >
			//service 0.1 ssh=vpn,mail,http=smtp,domain,OTHER,rtsp
			String[] split = StringUtil.trimmedSplit(line, " ");
			String name = split[0];
			double weight = Double.parseDouble(split[1]);
			String schema = split[2];
			Attribute att = instances.attribute(name);
			if (att == null) {
				Log.log(Level.INFO, "Feature " + name + " not built, skipping...");
				continue;
			} else {
//				Log.log(Level.INFO, "Loading feature label " + name + " ...");
			}
			
			FeatureClassifier c;
			if ("<".equals(schema) || ">".equals(schema)) {
				c = new NumericFeatureClassifier(att.index(), weight, schema);
			} else {
				c = new NominalFeatureClassifier(att.index(), weight, schema);
			}
			mFeatures.add(c);
		}
		in.close();
	}

	public double classifyInstance(Instance inst) {
		double sum = 0.0;
		for (FeatureClassifier c : mFeatures) {
			sum += c.classify(inst);
		}
		return sum;
	}

}
