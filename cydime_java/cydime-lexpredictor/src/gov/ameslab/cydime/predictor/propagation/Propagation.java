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

package gov.ameslab.cydime.predictor.propagation;

import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.community.Matrix;
import gov.ameslab.cydime.util.CUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Propagation extends FeatureSet {
	
	private static final Logger Log = Logger.getLogger(Propagation.class.getName());
	
	private List<String> mTrainIPs;
	private List<String> mTestIPs;
	private Map<String, Double> mTrainLabels;
	private Map<String, Integer> mIPComm;
	private Matrix<Double> mCommMatrix;
	
	private double[] mCommLabels;
	private double[] mCommTrainLabels;
	
	public Propagation(List<String> trainIPs, List<String> testIPs, Map<String, Double> trainLabels, Map<String, Integer> ipComm, Matrix<Double> commMatrix, String inPath, String outPath) {
		super(null, inPath, outPath);
		mTrainIPs = trainIPs;
		mTestIPs = testIPs;
		mTrainLabels = trainLabels;
		mIPComm = ipComm;
		mCommMatrix = commMatrix;
	}

	public Map<String, Double> run() throws IOException {
		Log.log(Level.INFO, "Processing Propagation...");
		initCommLabels();
		
		Log.log(Level.INFO, "Normalizing columns and rows...");
		columnRowNormalize(mCommMatrix);
		
		double[] newCommLabels = new double[mCommMatrix.getISize()];
		final double THRESHOLD = 0.001 * mCommLabels.length;
//		while (true) {
		for (int it = 0; it < 100; it++) {
			//Clamp original labels
			for (int i = 0; i < mCommTrainLabels.length; i++) {
				if (mCommTrainLabels[i] > 0.0) {
					mCommLabels[i] = mCommTrainLabels[i];
				}
			}
			
			double diff = propagate(mCommMatrix, mCommLabels, newCommLabels);
			System.arraycopy(newCommLabels, 0, mCommLabels, 0, newCommLabels.length);
			
			Log.log(Level.INFO, "Iteration ended with difference sum = {0}", diff);
			
			if (diff < THRESHOLD) break;
		}
		
		return convertLabels();
	}

	private void initCommLabels() {
		mCommLabels = new double[mCommMatrix.getISize()];
		int[] commSize = new int[mCommMatrix.getISize()];
		for (String ip : mTrainIPs) {
			double label = mTrainLabels.get(ip);
			Integer comm = mIPComm.get(ip);
			if (comm == null) continue;
			
			mCommLabels[comm] += label;
			commSize[comm]++;
		}
		
		for (int i = 0; i < mCommLabels.length; i++) {
			if (commSize[i] > 0) {
				mCommLabels[i] = mCommLabels[i] / commSize[i];
			}
		}
		
		mCommTrainLabels = new double[mCommMatrix.getISize()];
		for (int i = 0; i < mCommLabels.length; i++) {
			if (mCommLabels[i] > 0.0) {
				mCommTrainLabels[i] = mCommLabels[i];
			} else {
				mCommTrainLabels[i] = -1.0;
			}
		}
	}

	private Map<String, Double> convertLabels() {
		Map<String, Double> pred = CUtil.makeMap();
		for (String ip : mTestIPs) {
			Integer comm = mIPComm.get(ip);
			if (comm == null) {
				pred.put(ip, 0.0);
			} else {
				pred.put(ip, mCommLabels[comm]);
			}
		}
		return pred;
	}

	private static void columnRowNormalize(Matrix<Double> matrix) {
		Map<Integer, Map<Integer, Double>> ijMap = matrix.getMatrix();
		for (Integer i : CUtil.makeSet(ijMap.keySet())) {
			Map<Integer, Double> jMap = ijMap.get(i);
			double sum = 0.0;
			for (double v : jMap.values()) {
				sum += v;
			}
			
			for (Integer j : CUtil.makeSet(jMap.keySet())) {
				double v = jMap.get(j);				
				jMap.put(j, v / sum);
			}
		}
		
		matrix.updateTranspose();
		
		Map<Integer, Map<Integer, Double>> jiMap = matrix.getTranspose();
		for (Integer j : CUtil.makeSet(jiMap.keySet())) {
			Map<Integer, Double> iMap = jiMap.get(j);
			double sum = 0.0;
			for (double v : iMap.values()) {
				sum += v;
			}
			
			for (Integer i : CUtil.makeSet(iMap.keySet())) {
				double v = iMap.get(i);
				matrix.set(i, j, v / sum);
			}
		}
		
		matrix.updateTranspose();
		matrix.updateAdjacencyArrays();
	}

	private static double propagate(Matrix<Double> matrix, double[] oldLabels, double[] newLabels) {
		double diffSum = 0.0;
		double jLabelsSum = 0.0;
		for (int j = 0; j < newLabels.length; j++) {
			double sum = 0.0;
			int[] jNeighbors = matrix.getNeighborListOfJ(j);
			if (jNeighbors == null) continue;
			
			for (int n = 0; n < jNeighbors.length; n++) {
				int i = jNeighbors[n];
				double v = matrix.get(i, j);
				sum += oldLabels[i] * v;
			}
			
			diffSum += Math.abs(newLabels[j] - sum);
			newLabels[j] = sum;
			jLabelsSum += sum;
		}
		
		Log.log(Level.INFO, "Propagated sum = {0}", jLabelsSum);
		return diffSum;
	}

}
