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

package gov.ameslab.cydime.preprocess.netflow;

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.NormalizeLog;
import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.ReplaceMissingValues;
import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.StringToNominal;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FileUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class Netflow extends FeatureSet {
	
	private static final Logger Log = Logger.getLogger(Netflow.class.getName());
	
	private Map<String, Instance> mInstanceMap;
	private Instances mSchema;
	private int mNumAtts;
	
	public Netflow(List<String> allIPs, String inPath, String outPath) {
		super(allIPs, inPath, outPath);
		mInstanceMap = CUtil.makeMap();
		mSchema = new Instances("netflow", new ArrayList<Attribute>(), 0);
	}

	public InstanceDatabase run() throws IOException {
		readSchema();
		readFile();
		prepareInstances();
		
		WekaPreprocess.filterUnsuperivsed(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX,
				StringToNominal,
				ReplaceMissingValues);
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		
		WekaPreprocess.filterUnsuperivsed(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX,
				NormalizeLog);
		
		return new InstanceDatabase(mCurrentOutPath, mAllIPs);
	}

	private void readFile() throws IOException {
		Log.log(Level.INFO, "Processing netflow...");
		
		BufferedReader in = new BufferedReader(new FileReader(mCurrentInPath));
		String line = in.readLine();		
		while ((line = in.readLine()) != null) {
			Instance inst = new DenseInstance(mNumAtts);
			inst.setDataset(mSchema);
			String[] values = line.toLowerCase().split(",");
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].trim();
			}
			String ip = values[0];
			
			//src_cc,dst_cc
			if (values[1].equals("--") && values[2].equals("--")) {
				inst.setMissing(0);
			} else if (values[1].equals("--")) {
				inst.setValue(0, values[2]);
			} else {
				inst.setValue(0, values[1]);
			}
			
			for (int i = 3, ia = 1; i < values.length; i++, ia++) {
				Attribute a = inst.attribute(ia);
				
				if (a.isNumeric()) {
					try {
						double num = Double.parseDouble(values[i]);
						inst.setValue(ia, num);
					} catch (NumberFormatException ex) {
						System.err.println("Numeric value expected: " + values[i]);
						inst.setMissing(ia);
					}
				} else {
					inst.setValue(ia, values[i]);
				}
			}
			
			inst.setClassMissing();
			mInstanceMap.put(ip, inst);
		}
		
		in.close();
	}

	private void readSchema() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(mCurrentInPath));
		String line = in.readLine();
		String[] names = line.split(",");
		mNumAtts = names.length - 1; // src_cc,dst_cc merged into cc
		
		line = in.readLine();
		
		String[] values = line.toLowerCase().split(",");
		for (int i = 0; i < values.length; i++) {
			values[i] = values[i].trim();
		}
		
		mSchema.insertAttributeAt(new Attribute("cc", (List<String>) null), 0);
		
		for (int i = 3, ia = 1; i < values.length; i++, ia++) {
			Attribute newA = null;
			try {
				Double.parseDouble(values[i]);
				newA = new Attribute(names[i].trim());
			} catch (NumberFormatException ex) {
				newA = new Attribute(names[i].trim(), (List<String>) null);
			}
			
			mSchema.insertAttributeAt(newA, ia);
		}
		
		Attribute classAtt = new Attribute("class");
		mSchema.insertAttributeAt(classAtt, mNumAtts - 1);
		mSchema.setClassIndex(mNumAtts - 1);
		
		in.close();
	}

	private void prepareInstances() {
		Instances instances = new Instances(mSchema, 0);
		
		for (String ip : mAllIPs) {
			Instance inst = mInstanceMap.get(ip);
			inst.setClassMissing();
			instances.add(inst);
		}
		
		WekaPreprocess.save(instances, mCurrentOutPath + WekaPreprocess.ALL_SUFFIX);
	}

}
