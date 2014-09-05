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
import gov.ameslab.cydime.preprocess.service.ServiceParser;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BiPropagation extends FeatureSet {
	
	private static final Logger Log = Logger.getLogger(BiPropagation.class.getName());
	
	private List<String> mTrainIPs;
	private List<String> mTestIPs;
	private Map<String, Double> mTrainLabels;

	private IndexedList<String> mIntIPList;
	private IndexedList<String> mExtIPList;
	private Matrix<Double> mMatrixIntExt; // mMatrix[internal][external]
	private Matrix<Double> mMatrixExtInt; // mMatrix[external][internal]
	
	private double[] mIntLabels;
	private double[] mExtLabels;
	private boolean[] mIntLabelsTrain;
	private boolean[] mExtLabelsTrain;

	private Set<String> mNetregs;

	public BiPropagation(List<String> trainIPs, List<String> testIPs, Map<String, Double> trainLabels, String inPath, String outPath) {
		super(null, inPath, outPath);
		mTrainIPs = trainIPs;
		mTestIPs = testIPs;
		mTrainLabels = trainLabels;
	}

	public Map<String, Double> run() throws IOException {
		Log.log(Level.INFO, "Processing BiPropagation...");
		
		read();
		
		Log.log(Level.INFO, "Normalizing edge weights...");
//		NormalizeGaussian.normalizePos(mMatrixIntExt);
		normalizeLog();
		
		Log.log(Level.INFO, "Normalizing columns and rows...");
		makeTranspose();
		columnRowNormalize(mMatrixIntExt);
		columnRowNormalize(mMatrixExtInt);
		
//		saveMatrix(mMatrixIntExt, mIntIPList, mExtIPList, "int-ext.graph.csv");
//		saveMatrix(mMatrixExtInt, mExtIPList, mIntIPList, "ext-int.graph.csv");

		mIntLabelsTrain = new boolean[mIntLabels.length];
		mExtLabelsTrain = new boolean[mExtLabels.length];
		for (String ip : mTrainIPs) {
			int index = mExtIPList.getIndex(ip);
			if (index < 0) continue;
			
			double label = mTrainLabels.get(ip);
			mExtLabels[index] = label;
			mExtLabelsTrain[index] = true;
		}
		
		final double THRESHOLD = 0.000001 * mExtLabels.length;
		while (true) {
//		for (int it = 0; it < 100; it++) {
			propagateItoJ(mMatrixExtInt, mExtLabels, mIntLabels, mIntLabelsTrain);
			double diff = propagateItoJ(mMatrixIntExt, mIntLabels, mExtLabels, mExtLabelsTrain);
			
			Log.log(Level.INFO, "Iteration ended with difference sum = {0}", diff);
			
			if (diff < THRESHOLD) break;
		}
		
		Map<String, Double> pred = CUtil.makeMap();
		for (String ip : mTestIPs) {
			int index = mExtIPList.getIndex(ip);
			if (index >= 0) {
				pred.put(ip, mExtLabels[index]);
			} else {
				pred.put(ip, 0.0);
			}
		}
		
		return pred;
	}
	
	private void read() throws IOException {
		mNetregs = CUtil.makeSet(FileUtil.readFile(Config.INSTANCE.getCurrentPreprocessPath() + "int.csv"));

		Set<String> allExt = CUtil.makeSet();
		allExt.addAll(mTrainIPs);
		allExt.addAll(mTestIPs);
		
		Set<String> intSet = CUtil.makeSet();
		Set<String> extSet = CUtil.makeSet();
		for (String inPath : mFeaturePaths) {
			Log.log(Level.INFO, "Reading {0}", inPath);
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				if (filterOut(split, allExt)) continue;
				
				intSet.add(split[0]);
				extSet.add(split[1]);
			}
			in.close();
		}
		
		mIntIPList = new IndexedList<String>(intSet);
		mExtIPList = new IndexedList<String>(extSet);
		
		Log.log(Level.INFO, "External IPs = {0}", mExtIPList.size());
		Log.log(Level.INFO, "Internal IPs = {0}", mIntIPList.size());
		
		mMatrixIntExt = new Matrix<Double>(mIntIPList.size(), mExtIPList.size(), 0.0);
		for (String inPath : mFeaturePaths) {
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				if (filterOut(split, allExt)) continue;
				
				double weight = Double.parseDouble(split[6]);
				int intIndex = mIntIPList.getIndex(split[0]);
				int extIndex = mExtIPList.getIndex(split[1]);
				Double old = mMatrixIntExt.get(intIndex, extIndex);
				mMatrixIntExt.set(intIndex, extIndex, old + weight);
			}
			in.close();
		}
		
		mIntLabels = new double[mIntIPList.size()];
		mExtLabels = new double[mExtIPList.size()];
	}

	private boolean filterOut(String[] split, Set<String> allExt) {
//		if (mScanners.contains(split[1])) return true;
		if (!mNetregs.contains(split[0])) return true;
		
		if (!allExt.contains(split[1])) return true;
		
		String src = split[2];
		String dest = split[3];
		Set<String> services = ServiceParser.parse(src, dest);
		if (services.contains(ServiceParser.SERVICE_DOMAIN)
				|| services.contains(ServiceParser.SERVICE_HTTP)
				|| services.contains(ServiceParser.SERVICE_MAIL)
				|| services.contains(ServiceParser.SERVICE_SMTP)
				|| services.contains(ServiceParser.SERVICE_SSH)
				|| services.contains(ServiceParser.SERVICE_VPN)) {
			return false;
		} else return true;
		
//		if (Double.parseDouble(split[6]) < 256) return true;
//		return false;
	}

	private void normalizeLog() {
		Map<Integer, Map<Integer, Double>> ijMap = mMatrixIntExt.getMatrix();
		for (Integer i : CUtil.makeSet(ijMap.keySet())) {
			Map<Integer, Double> jMap = ijMap.get(i);
			for (Integer j : CUtil.makeSet(jMap.keySet())) {
				double v = jMap.get(j);				
				jMap.put(j, Math.log(v+1.0));
			}
		}
		
		//Normalize for both in-degree and out-degree
//		double[] jSums = new double[mMatrixIntExt.getJSize()];
//		ijMap = mMatrixIntExt.getMatrix();
//		for (Integer i : ijMap.keySet()) {
//			Map<Integer, Double> jMap = ijMap.get(i);
//			for (Integer j : jMap.keySet()) {
//				jSums[j] += jMap.get(j);				
//			}
//		}
//		
//		ijMap = mMatrixIntExt.getMatrix();
//		for (Integer i : CUtil.makeSet(ijMap.keySet())) {
//			Map<Integer, Double> jMap = ijMap.get(i);
//			double sum = 0.0;
//			for (double v : jMap.values()) {
//				sum += v;
//			}
//			
//			for (Integer j : CUtil.makeSet(jMap.keySet())) {
//				double v = jMap.get(j);				
//				jMap.put(j, v / (sum + jSums[j]));
//			}
//		}
	}

	private void saveMatrix(Matrix<Double> matrix, IndexedList<String> ipListI, IndexedList<String> ipListJ, String file) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(mCurrentOutPath + "." + file));
		for (int j = 0; j < ipListJ.size(); j++) {
			for (int i : matrix.getNeighborsOfJ(j)) {
				String ipI = ipListI.get(i);
				String ipJ = ipListJ.get(j);
				out.write(ipI);
				out.write(",");
				out.write(ipJ);
				out.write(",");
				out.write(Double.toString(matrix.get(i, j)));
				out.newLine();
			}
		}
		out.close();
		
		out = new BufferedWriter(new FileWriter(mCurrentOutPath + "." + file + ".adj"));
		for (int j = 0; j < ipListJ.size(); j++) {
			String ipJ = ipListJ.get(j);
			out.write(ipJ);
			out.write("," + matrix.getNeighborsOfJ(j).size());
			out.newLine();
		}
		out.close();
	}

	private void makeTranspose() {
		mMatrixExtInt = new Matrix<Double>(mExtIPList.size(), mIntIPList.size(), 0.0);
		
		for (Entry<Integer, Map<Integer, Double>> iEntry : mMatrixIntExt.getMatrix().entrySet()) {
			int i = iEntry.getKey();
			for (Entry<Integer, Double> jEntry : iEntry.getValue().entrySet()) {
				int j = jEntry.getKey();
				Double value = jEntry.getValue();
				mMatrixExtInt.set(j, i, value);
			}
		}
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

	private static double propagateItoJ(Matrix<Double> matrix, double[] iLabels, double[] jLabels, boolean[] jLabelsTrain) {
		double diffSum = 0.0;
		double jLabelsSum = 0.0;
		for (int j = 0; j < jLabels.length; j++) {
			if (jLabelsTrain[j]) continue;
			
			double sum = 0.0;
			int[] jNeighbors = matrix.getNeighborListOfJ(j);
			for (int n = 0; n < jNeighbors.length; n++) {
				int i = jNeighbors[n];
				double v = matrix.get(i, j);
				sum += iLabels[i] * v;
			}
			
			diffSum += Math.abs(jLabels[j] - sum);
			jLabels[j] = sum;
			jLabelsSum += sum;
		}
		
//		Log.log(Level.INFO, "Propagated sum = {0}", jLabelsSum);
		return diffSum;
	}

}
