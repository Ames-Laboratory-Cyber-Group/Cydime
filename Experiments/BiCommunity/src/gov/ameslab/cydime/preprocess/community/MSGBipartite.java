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

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Histogram;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MSGBipartite {

	class Pair implements Comparable<Pair> {

		public int ILabel;
		public int JLabel;
		
		private double mDelta;
		
		public Pair(int iLabel, int jLabel) {
			ILabel = iLabel;
			JLabel = jLabel;
			computeDelta();
		}

		@Override
		public int compareTo(Pair o) {
			return Double.compare(o.mDelta, mDelta);
		}

		public double getDelta() {
			return mDelta;
		}

		private void computeDelta() {
			double merged = cLabelMatrix.get(ILabel, ILabel)
					+ cLabelMatrix.get(JLabel, JLabel)
					+ cLabelMatrix.get(ILabel, JLabel)
					+ cLabelMatrix.get(JLabel, ILabel);
			merged -= (cIntDegSumForLabel.get(ILabel) + cIntDegSumForLabel.get(JLabel))
					* (cExtDegSumForLabel.get(ILabel) + cExtDegSumForLabel.get(JLabel))
					/ mSum;
			
			mDelta = merged - cLabelModularity.get(ILabel) - cLabelModularity.get(JLabel);
		}
		
		@Override
		public String toString() {
			return new StringBuilder()
				.append("(")
				.append(ILabel)
				.append(",")
				.append(JLabel)
				.append(")").toString();
		}

	}
	
	private static final Logger Log = Logger.getLogger(MSGBipartite.class.getName());
	
	private Matrix<Double> mMatrix;
	private double mSum;

	private int[] cIntLabel;
	private int[] cExtLabel;
	private Set<Integer> cLabels;
	private Matrix<Double> cLabelMatrix;
	private Histogram<Integer> cIntDegSumForLabel;
	private Histogram<Integer> cExtDegSumForLabel;
	private Map<Integer, Double> cLabelModularity;
	
	public MSGBipartite(Matrix<Double> matrix, double sum) {
		mMatrix = matrix;
		mSum = sum;
	}
	
	public void run(int[] intLabel, int[] extLabel) {
		Log.log(Level.INFO, "Running MSGBipartite...");
		
		cIntLabel = intLabel;
		cExtLabel = extLabel;
		
		initLabels();
		initDegSum();
		initLabelModularity();
		
		Log.log(Level.INFO, "Labels = {0}", cLabels.size());
		Log.log(Level.INFO, "Modularity = {0}", getModularity());
		
		List<Pair> pairs = makePairs();
		Set<Integer> mergedLabels = CUtil.makeSet();
		List<Pair> mergedPairs = CUtil.makeList();
		
		while (!pairs.isEmpty()) {
			Collections.sort(pairs);
			mergedLabels.clear();
			for (Pair p : pairs) {
				if (mergedLabels.contains(p.ILabel) || mergedLabels.contains(p.JLabel)) continue;
				
				mergeLabels(p.ILabel, p.JLabel);
				mergedLabels.add(p.ILabel);
				mergedLabels.add(p.JLabel);
				mergedPairs.add(p);
			}
			
			cLabelMatrix.updateTranspose();
			
			pairs = makePairs();
		}
		
//		Log.log(Level.INFO, "Merged Pairs = {0}", mergedPairs.toString());
		Log.log(Level.INFO, "Labels = {0}", cLabels.size());
		Log.log(Level.INFO, "Modularity = {0}", getModularity());
	}

	private void initLabels() {
		cLabels = CUtil.makeSet();
		for (int i = 0; i < cIntLabel.length; i++) {
			cLabels.add(cIntLabel[i]);
		}
		
		cLabelMatrix = new Matrix<Double>(cLabels.size(), cLabels.size(), 0.0);
		for (Entry<Integer, Map<Integer, Double>> iEntry : mMatrix.getMatrix().entrySet()) {
			int i = iEntry.getKey();
			int iLabel = cIntLabel[i];
			for (Entry<Integer, Double> jEntry : iEntry.getValue().entrySet()) {
				int j = jEntry.getKey();
				int jLabel = cExtLabel[j];
				double w = jEntry.getValue();
				
				Double old = cLabelMatrix.get(iLabel, jLabel);
				cLabelMatrix.set(iLabel, jLabel, old + w);
			}
		}
		
		cLabelMatrix.updateTranspose();
	}

	private void initDegSum() {
		cIntDegSumForLabel = new Histogram<Integer>();
		cExtDegSumForLabel = new Histogram<Integer>();
		
		for (Entry<Integer, Map<Integer, Double>> iEntry : mMatrix.getMatrix().entrySet()) {
			int i = iEntry.getKey();
			int iLabel = cIntLabel[i];
			for (Entry<Integer, Double> jEntry : iEntry.getValue().entrySet()) {
				int j = jEntry.getKey();
				int jLabel = cExtLabel[j];
				double w = jEntry.getValue();
				
				cIntDegSumForLabel.increment(iLabel, w);
				cExtDegSumForLabel.increment(jLabel, w);
			}
		}
	}
	
	private void initLabelModularity() {
		cLabelModularity = CUtil.makeMap();
		
		for (int label : cLabels) {
			cLabelModularity.put(label, getLabelModularity(label));
		}
	}
	
	private double getLabelModularity(int label) {
		return cLabelMatrix.get(label, label) - cIntDegSumForLabel.get(label) * cExtDegSumForLabel.get(label) / mSum;
	}
	
	private List<Pair> makePairs() {
		List<Pair> pairs = CUtil.makeList();
		
		List<Integer> labelList = CUtil.makeList(cLabels);
		Collections.sort(labelList);
		Set<Integer> neighbors = CUtil.makeSet();
		for (int i = 0; i < labelList.size() - 1; i++) {
			int iLabel = labelList.get(i);
			neighbors.clear();
			neighbors.addAll(cLabelMatrix.getNeighborsOfI(iLabel));
			neighbors.addAll(cLabelMatrix.getNeighborsOfJ(iLabel));
			for (int jLabel : neighbors) {
				if (jLabel <= iLabel) continue;
				
				Pair p = new Pair(iLabel, jLabel);
				if (p.getDelta() > 0.0) {
					pairs.add(p);
				}
			}
		}
		
		return pairs;
	}

	private void mergeLabels(int iLabel, int jLabel) {
		//cLabelMatrix
		for (int other : cLabels) {
			double w = cLabelMatrix.get(iLabel, other) + cLabelMatrix.get(jLabel, other);
			cLabelMatrix.set(iLabel, other, w);
		}
		
		for (int other : cLabels) {
			double w = cLabelMatrix.get(other, iLabel) + cLabelMatrix.get(other, jLabel);
			cLabelMatrix.set(other, iLabel, w);
		}
		
		cLabelMatrix.removeI(jLabel);
		cLabelMatrix.removeJ(jLabel);
		
		//cIntDegSumForLabel cExtDegSumForLabel
		double s = cIntDegSumForLabel.get(jLabel);
		cIntDegSumForLabel.increment(iLabel, s);
		cIntDegSumForLabel.remove(jLabel);
		
		s = cExtDegSumForLabel.get(jLabel);
		cExtDegSumForLabel.increment(iLabel, s);
		cExtDegSumForLabel.remove(jLabel);
		
		//cLabelModularity
		cLabelModularity.put(iLabel, getLabelModularity(iLabel));
		cLabelModularity.remove(jLabel);
		
		//cIntLabel cExtLabel
		for (int i = 0; i < cIntLabel.length; i++) {
			if (cIntLabel[i] == jLabel) {
				cIntLabel[i] = iLabel;
			}
		}
		
		for (int i = 0; i < cExtLabel.length; i++) {
			if (cExtLabel[i] == jLabel) {
				cExtLabel[i] = iLabel;
			}
		}
		
		//cLabels
		cLabels.remove(jLabel);
	}

	private double getModularity() {
		double sum = 0.0;
		for (double m : cLabelModularity.values()) {
			sum += m;
		}
		
		return sum / mSum;
	}
	
}
