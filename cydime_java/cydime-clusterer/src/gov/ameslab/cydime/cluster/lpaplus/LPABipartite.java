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

package gov.ameslab.cydime.cluster.lpaplus;

import gov.ameslab.cydime.cluster.Algorithm;
import gov.ameslab.cydime.cluster.Dataset;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FastListSet;
import gov.ameslab.cydime.util.Histogram;
import gov.ameslab.cydime.util.IndexedList;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LPABipartite implements Algorithm {

	private static final Logger Log = Logger.getLogger(LPABipartite.class.getName());
	
	private static final double MIN_MOD_INCREMENT = 0.0001;
	
	private LPADataset mData;
	private double mGamma = 1.0;
	private Histogram<Integer> cIntDegSumForLabel;
	private Histogram<Integer> cExtDegSumForLabel;
	
	private Histogram<Integer> tScoreForLabel;
	private FastListSet<Integer> tLabels;


	@Override
	public String getName() {
		return "modularity";
	}


	@Override
	public Dataset run(String graphFile, double gamma) throws IOException {
		mData = LPADataset.load(graphFile);
		mGamma = gamma;
		
		Log.log(Level.INFO, "Running LPABipartite with Gamma = {0}...", mGamma);
		Log.log(Level.INFO, "Internal size = " + mData.mIntLabel.length + " External size = " + mData.mExtLabel.length);
		
		tScoreForLabel = new Histogram<Integer>();
		tLabels = new FastListSet<Integer>();
		
		init();
		double modularity = getModularity();
		while (true) {
//		for (int it = 0; it < 1; it++) {
			Log.log(Level.INFO, "Modularity = {0}", modularity);
			
			for (int i = 0; i < mData.mIntLabel.length; i++) {
				cIntDegSumForLabel.increment(mData.mIntLabel[i], -mData.cIntDegs[i]);
				mData.mIntLabel[i] = updateIntLabel(i);
				cIntDegSumForLabel.increment(mData.mIntLabel[i], mData.cIntDegs[i]);
			}
			
			for (int j = 0; j < mData.mExtLabel.length; j++) {
				cExtDegSumForLabel.increment(mData.mExtLabel[j], -mData.cExtDegs[j]);
				mData.mExtLabel[j] = updateExtLabel(j);
				cExtDegSumForLabel.increment(mData.mExtLabel[j], mData.cExtDegs[j]);
			}
			
			double newModularity = getModularity();
			if (newModularity - modularity < MIN_MOD_INCREMENT) {
				break;
			} else {
				modularity = newModularity;
			}
		}
		
		relabel();
		return mData;
	}
	
	private void init() {
		int c = 0;
		for (int i = 0; i < mData.mIntLabel.length; i++) {
			mData.mIntLabel[i] = c++;
		}
		for (int i = 0; i < mData.mExtLabel.length; i++) {
			mData.mExtLabel[i] = c++;
		}
		
		cIntDegSumForLabel = new Histogram<Integer>();
		cExtDegSumForLabel = new Histogram<Integer>();
		
		for (int i = 0; i < mData.mIntLabel.length; i++) {
			cIntDegSumForLabel.increment(mData.mIntLabel[i], mData.cIntDegs[i]);
		}
		
		for (int j = 0; j < mData.mExtLabel.length; j++) {
			cExtDegSumForLabel.increment(mData.mExtLabel[j], mData.cExtDegs[j]);
		}
	}

	private void relabel() {
		Set<Integer> labels = CUtil.makeSet();
		for (int i = 0; i < mData.mIntLabel.length; i++) {
			labels.add(mData.mIntLabel[i]);
		}
		for (int i = 0; i < mData.mExtLabel.length; i++) {
			labels.add(mData.mExtLabel[i]);
		}
		
		Log.log(Level.INFO, "Communities = {0}", labels.size());
		
		IndexedList<Integer> labelList = new IndexedList<Integer>(labels);
		for (int i = 0; i < mData.mIntLabel.length; i++) {
			mData.mIntLabel[i] = labelList.getIndex(mData.mIntLabel[i]);
		}
		for (int i = 0; i < mData.mExtLabel.length; i++) {
			mData.mExtLabel[i] = labelList.getIndex(mData.mExtLabel[i]);
		}
	}
	
	private double getModularity() {
		double result = 0.0;
		
		for (Entry<Integer, Map<Integer, Double>> iEntry : mData.mMatrixIntExt.getMatrix().entrySet()) {
			int i = iEntry.getKey();
			int iLabel = mData.mIntLabel[i];
			Map<Integer, Double> jMap = iEntry.getValue();
			
			int[] iNeighbors = mData.mMatrixIntExt.getNeighborListOfI(i);
			for (int n = 0; n < iNeighbors.length; n++) {
				int j = iNeighbors[n];
				int jLabel = mData.mExtLabel[j];
				
				if (iLabel == jLabel) {
					result += jMap.get(j);
				}
			}
		}
		
		for (int label : cIntDegSumForLabel.keySet()) {
			result -= mGamma * cIntDegSumForLabel.get(label) * cExtDegSumForLabel.get(label) / mData.cMatrixSum;
		}
		
		return result / mData.cMatrixSum;
	}

	
	private int updateIntLabel(int i) {
		int[] iNeighbors = mData.mMatrixIntExt.getNeighborListOfI(i);
		
		if (iNeighbors.length == 1) {
			return mData.mExtLabel[iNeighbors[0]];
		}
		
		tLabels.clear();
		
		for (int n = 0; n < iNeighbors.length; n++) {
			int j = iNeighbors[n];
			int jLabel = mData.mExtLabel[j];
			double w = mData.mMatrixIntExt.get(i, j);
			tScoreForLabel.increment(jLabel, w);
			tLabels.add(jLabel);
		}
		
		int maxLabel = -1;
		double max = -Double.MAX_VALUE;
		for (int a = 0; a < tLabels.size(); a++) {
			int jLabel = tLabels.get(a);
			
			double w = tScoreForLabel.get(jLabel) - mGamma * mData.cIntDegs[i] * cExtDegSumForLabel.get(jLabel) / mData.cMatrixSum;
			if (w > max) {
				max = w;
				maxLabel = jLabel;
			}
			
			tScoreForLabel.remove(jLabel);
		}
		
		return maxLabel;
	}
	
	private int updateExtLabel(int j) {
		int[] jNeighbors = mData.mMatrixIntExt.getNeighborListOfJ(j);
		
		if (jNeighbors.length == 1) {
			return mData.mIntLabel[jNeighbors[0]];
		}
		
		tLabels.clear();
		
		for (int n = 0; n < jNeighbors.length; n++) {
			int i = jNeighbors[n];
			int iLabel = mData.mIntLabel[i];
			double w = mData.mMatrixIntExt.get(i, j);
			tScoreForLabel.increment(iLabel, w);
			tLabels.add(iLabel);
		}
		
		int maxLabel = -1;
		double max = -Double.MAX_VALUE;
		for (int a = 0; a < tLabels.size(); a++) {
			int iLabel = tLabels.get(a);
			
			double w = tScoreForLabel.get(iLabel) - mGamma * mData.cExtDegs[j] * cIntDegSumForLabel.get(iLabel) / mData.cMatrixSum;
			if (w > max) {
				max = w;
				maxLabel = iLabel;
			}
			
			tScoreForLabel.remove(iLabel);
		}
		
		return maxLabel;
	}

}
