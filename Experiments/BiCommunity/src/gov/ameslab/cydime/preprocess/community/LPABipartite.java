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
import gov.ameslab.cydime.util.FastListSet;
import gov.ameslab.cydime.util.Histogram;
import gov.ameslab.cydime.util.IndexedList;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LPABipartite {

	private static final Logger Log = Logger.getLogger(LPABipartite.class.getName());
	
	private static final double MIN_MOD_INCREMENT = 0.0001;
	
	private Matrix<Double> mMatrix;
	private double mSum;
	private double[] mIntDegs;
	private double[] mExtDegs;
	private String basePath;

	private int[] cIntLabel;
	private int[] cExtLabel;
	private Histogram<Integer> cIntDegSumForLabel;
	private Histogram<Integer> cExtDegSumForLabel;
	
	private Histogram<Integer> tScoreForLabel;
	private FastListSet<Integer> tLabels;
	private IndexedList<String> indexedGroups;
	private IndexedList<Integer> asnList;
	
	public LPABipartite(Matrix<Double> matrix, double sum, double[] intDegs, double[] extDegs,String basePath) {
		mMatrix = matrix;
		mSum = sum;
		mIntDegs = intDegs;
		mExtDegs = extDegs;
		this.basePath = basePath;
	}
	
	public void run(int[] intLabel, int[] extLabel, IndexedList<String> indexedGroups, IndexedList<Integer> asnList,String service,String day) throws IOException{
		Log.log(Level.INFO, "Running LPABipartite...");
		this.indexedGroups=indexedGroups;
		this.asnList=asnList;
		cIntLabel = intLabel;
		cExtLabel = extLabel;
		tScoreForLabel = new Histogram<Integer>();
		tLabels = new FastListSet<Integer>();
		
		initDegSum();

		double modularity = getModularity();
		Log.log(Level.INFO, "Modularity before :"+ modularity);
//mahee
// 		while (true) {
////		for (int it = 0; it < 1; it++) {
//			Log.log(Level.INFO, "Modularity = {0}", modularity);
//
//			for (int i = 0; i < cIntLabel.length; i++) {
//				cIntDegSumForLabel.increment(cIntLabel[i], -mIntDegs[i]);
//				cIntLabel[i] = updateIntLabel(i);//label update
//				cIntDegSumForLabel.increment(cIntLabel[i], mIntDegs[i]);
//			}
//
		for (int j = 0; j < cExtLabel.length; j++) {
			cExtDegSumForLabel.increment(cExtLabel[j], -mExtDegs[j]);
			cExtLabel[j] = updateExtLabel(j);//label update
			cExtDegSumForLabel.increment(cExtLabel[j], mExtDegs[j]);
		}
//
		double newModularity = getModularity();
		Log.log(Level.INFO, "Modularity after :"+ newModularity);
		outputCommunities(getModularityMap(),service,day);
//			if (newModularity - modularity < MIN_MOD_INCREMENT) {
//				break;
//			} else {
//				modularity = newModularity;
//			}
//		}
///mahee
	}



	private void outputCommunities(Histogram<Integer> modularityMap,String service, String day) throws IOException{
		List<Integer> sortedModularityList = modularityMap.getSortedKeysByValue();
		Collections.reverse(sortedModularityList);
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(basePath+"intermediateOutput_Subnet_"+day+"_"+service+".bic")));
		InputGroup inputGroup = new InputGroup(basePath);
		HashMap<Integer,String> externalHostNames = inputGroup.getASNNumberToASNNameMap();
		bw.write("\nPartition name,Modularity,External ASN(CSV list)");
		for (int key : sortedModularityList) {
			bw.write("\n"+indexedGroups.get(key)+","+modularityMap.get(key));
			//this is going to be ugly
			Set<String> asns = CUtil.makeSet();
			for (int j = 0; j < cExtLabel.length; j++) {
				if(cExtLabel[j]==key){
					bw.write(","+asnList.get(j));
				}
			}
		}
		bw.close();
	}

	private void initDegSum() {
		int i=0;
		try{
			cIntDegSumForLabel = new Histogram<Integer>(); //calculate the sum of degree of the nodes for the community with a particular label
			cExtDegSumForLabel = new Histogram<Integer>();

			long edges = 0;
			for (Entry<Integer, Map<Integer, Double>> iEntry : mMatrix.getMatrix().entrySet()) {
				i = iEntry.getKey();
				int iLabel = cIntLabel[i];
				for (Entry<Integer, Double> jEntry : iEntry.getValue().entrySet()) {
					int j = jEntry.getKey();
					int jLabel = cExtLabel[j];
					double w = jEntry.getValue();

					cIntDegSumForLabel.increment(iLabel, w);//increment by the weight for degree - since this is the weighted version
					cExtDegSumForLabel.increment(jLabel, w);//increment by the weight for degree - since this is the weighted version
					edges++;
				}
			}

			Log.log(Level.INFO, "Edges = {0}", edges);
		}catch (ArrayIndexOutOfBoundsException e){
			System.out.println(i);
		}
	}
	
	private double getModularity() {
		double result = 0.0;
		
		for (Entry<Integer, Map<Integer, Double>> iEntry : mMatrix.getMatrix().entrySet()) {
			int i = iEntry.getKey();
			int iLabel = cIntLabel[i];
			Map<Integer, Double> jMap = iEntry.getValue();
			
			int[] iNeighbors = mMatrix.getNeighborListOfI(i);
			for (int n = 0; n < iNeighbors.length; n++) {
				int j = iNeighbors[n];
				int jLabel = cExtLabel[j];
				
				if (iLabel == jLabel) {
					result += jMap.get(j);
				}
			}
		}
		
		for (int label : cIntDegSumForLabel.keySet()) {
			result -= cIntDegSumForLabel.get(label) * cExtDegSumForLabel.get(label) / mSum;
		}
		
		return result / mSum;
	}

	private Histogram<Integer> getModularityMap() {
		Histogram<Integer> modularityMap = new Histogram<Integer>();

		for (Entry<Integer, Map<Integer, Double>> iEntry : mMatrix.getMatrix().entrySet()) {
			double result = 0.0;
			int i = iEntry.getKey();
			int iLabel = cIntLabel[i]; //label of the node identified by "i"
			Map<Integer, Double> jMap = iEntry.getValue();

			int[] iNeighbors = mMatrix.getNeighborListOfI(i);
			for (int n = 0; n < iNeighbors.length; n++) {
				int j = iNeighbors[n];
				int jLabel = cExtLabel[j];

				if (iLabel == jLabel) {
					result += jMap.get(j);
					//result -= cIntDegSumForLabel.get(iLabel) * cExtDegSumForLabel.get(iLabel) / mSum;
				}
			}

			modularityMap.increment(iLabel, result / mSum);
		}

		for (int label : cIntDegSumForLabel.keySet()) {
			modularityMap.increment(label, -cIntDegSumForLabel.get(label) * cExtDegSumForLabel.get(label) / mSum / mSum);
		}

//		System.out.println(modularityMap.sum() + " " + getModularity());

		return modularityMap;
	}

	
	private int updateIntLabel(int i) {// formula for label updation of internal nodes
		int[] iNeighbors = mMatrix.getNeighborListOfI(i);
		
		if (iNeighbors.length == 1) {
			return cExtLabel[iNeighbors[0]];
		}
		
		tLabels.clear();
		
		for (int n = 0; n < iNeighbors.length; n++) {
			int j = iNeighbors[n];
			int jLabel = cExtLabel[j];
			double w = mMatrix.get(i, j);
			tScoreForLabel.increment(jLabel, w);
			tLabels.add(jLabel);
		}
		
		int maxLabel = -1;
		double max = -Double.MAX_VALUE;
		for (int a = 0; a < tLabels.size(); a++) {
			int jLabel = tLabels.get(a);
			
			double w = tScoreForLabel.get(jLabel) - mIntDegs[i] * cExtDegSumForLabel.get(jLabel) / mSum;
			if (w > max) {
				max = w;
				maxLabel = jLabel;
			}
			
			tScoreForLabel.remove(jLabel);
		}
		
		return maxLabel;
	}
	
	private int updateExtLabel(int j) {
		try{
			int[] jNeighbors = mMatrix.getNeighborListOfJ(j);

			if(jNeighbors!=null){
				if (jNeighbors.length == 1) {
					return cIntLabel[jNeighbors[0]];
				}

				tLabels.clear();

				for (int n = 0; n < jNeighbors.length; n++) {
					int i = jNeighbors[n];
					int iLabel = cIntLabel[i];
					double w = mMatrix.get(i, j);
					tScoreForLabel.increment(iLabel, w);
					tLabels.add(iLabel);
				}

				int maxLabel = -1;
				double max = -Double.MAX_VALUE;
				for (int a = 0; a < tLabels.size(); a++) {
					int iLabel = tLabels.get(a);

					double w = tScoreForLabel.get(iLabel) - mExtDegs[j] * cIntDegSumForLabel.get(iLabel) / mSum;
					if (w > max) {
						max = w;
						maxLabel = iLabel;
					}

					tScoreForLabel.remove(iLabel);
				}
				return maxLabel;
			}
			return -200; //so that all the neighbour less nodes dont form their own communities - yucky hack!!
		}catch (Exception e){
			System.out.println(j);
			return 0;
		}
	}

	private static class Record<K> implements Comparable<Record<K>> {
		K Key;
		Double Value;

		public Record(K key, Double value) {
			Key = key;
			Value = value;
		}
		@Override
		public int compareTo(Record<K> o) {
			return Value.compareTo(o.Value);
		}
	}

	public static <K> List<K> getSortedKeysByValue(final Map<K, Double> as) {
		List<Record<K>> records = CUtil.makeList();
		for (Map.Entry<K, Double> entry : as.entrySet()) {
			records.add(new Record<K>(entry.getKey(), entry.getValue()));
		}
		Collections.sort(records);

		List<K> sorted = CUtil.makeList();
		for (Record<K> r : records) {
			sorted.add(r.Key);
		}
		return sorted;
	}


}
