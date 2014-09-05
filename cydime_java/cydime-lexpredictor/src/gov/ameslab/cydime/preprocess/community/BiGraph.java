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

package gov.ameslab.cydime.preprocess.community;

import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BiGraph extends FeatureSet {
	
	private static final Logger Log = Logger.getLogger(BiGraph.class.getName());
	
	private IndexedList<String> mIntIPList;
	private IndexedList<String> mExtIPList;
	private Matrix<Double> mMatrix; // mMatrix[internal][external]
	
	public BiGraph(List<String> ipList, String inPath, String outPath) {
		super(ipList, inPath, outPath);
	}

	public void run() throws IOException {
		Log.log(Level.INFO, "Processing BiGraph...");
		
		read();
		saveMatrix();
	}
	
	private void read() throws IOException {
		Set<String> intSet = CUtil.makeSet();
		Set<String> extSet = CUtil.makeSet();
		for (String inPath : mFeaturePaths) {
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				intSet.add(split[0]);
				extSet.add(split[1]);
			}
			in.close();
		}
		
		mIntIPList = new IndexedList<String>(intSet);
		mExtIPList = new IndexedList<String>(extSet);
		
		Log.log(Level.INFO, "External IPs = {0}", mExtIPList.size());
		Log.log(Level.INFO, "Internal IPs = {0}", mIntIPList.size());
		
		mMatrix = new Matrix<Double>(mIntIPList.size(), mExtIPList.size(), 0.0);
		for (String inPath : mFeaturePaths) {
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				double weight = Double.parseDouble(split[6]);
				int intIndex = mIntIPList.getIndex(split[0]);
				int extIndex = mExtIPList.getIndex(split[1]);
				Double old = mMatrix.get(intIndex, extIndex);
				mMatrix.set(intIndex, extIndex, old + weight);
			}
			in.close();
		}
	}

	private void saveMatrix() throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(mCurrentOutPath + ".graph.csv"));
		for (int i = 0; i < mIntIPList.size(); i++) {
			for (int e : mMatrix.getNeighborsOfI(i)) {
				String intIP = mIntIPList.get(i);
				String extIP = mExtIPList.get(e);
				out.write(intIP);
				out.write(",");
				out.write(extIP);
				out.newLine();
			}
		}
		out.close();
	}

}
