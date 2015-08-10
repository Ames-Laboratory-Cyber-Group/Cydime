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

import gov.ameslab.cydime.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BiCommunity {

	private static final Logger Log = Logger.getLogger(BiCommunity.class.getName());

	private IndexedList<String> mIntIPList;
	private IndexedList<String> mExtIPList;
	private Matrix<Double> mMatrix; // mMatrix[internal][external]
	private int mCommSize;
	private int[] mIntLabel;
	private int[] mExtLabel;
	private Histogram<Integer> mAllIntLabels;
	private Histogram<Integer> mAllExtLabels;
	private double[] mExtFocusScore;
	private Map<Integer, Double> mExtLabelRatio;
	private String basePath;
	private IndexedList<Integer> asnList;
	private double cMatrixSum;
	private double[] cMatrixIntDegs;
	private double[] cMatrixExtDegs;
	HashMap<Integer,Integer> ExtIpToASNMap;

	public String getBasePath() {
		return basePath;
	}

	public IndexedList<Integer> getAsnList() {
		return asnList;
	}

	public void setAsnList(IndexedList<Integer> asnList) {
		this.asnList = asnList;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public Matrix<Double> getmMatrix() {
		return mMatrix;
	}

	public double[] getcMatrixIntDegs() {
		return cMatrixIntDegs;
	}

	public double[] getcMatrixExtDegs() {
		return cMatrixExtDegs;
	}

	public double getcMatrixSum() {
		return cMatrixSum;
	}

	public int[] getmExtLabel() {
		return mExtLabel;
	}

	public int[] getmIntLabel() {
		return mIntLabel;
	}

	public IndexedList<String> getmExtIPList() {
		return mExtIPList;
	}

	public IndexedList<String> getmIntIPList() {
		return mIntIPList;
	}

	public static void main(String args[]) throws IOException {
		BiCommunity biCommunity = new BiCommunity();
		biCommunity.setBasePath("/home/maheedhar/cluster/07/");
		InputGroup inputGroup = new InputGroup(biCommunity.getBasePath());
		IndexedList<String> indexedGroups = inputGroup.getIndexedGroups();
		HashMap<String,Integer> mappings = inputGroup.getAllGroupMappings(indexedGroups);
		biCommunity.setAsnList(inputGroup.getASNList());
		for (String service : ServiceParser.SERVICES){
			String[] list = {"01","06","08",  "10",  "14" , "16"  ,"20" , "22",  "24" , "28"  ,"30",
					"02" , "07",  "09",  "13",  "15",  "17",  "21",  "23",  "27",  "29",  "31"};
			for(String day : list) {
				Log.log(Level.INFO, "Processing BiCommunity for "+service+" for the date 2015/07/"+day);
				biCommunity.read(mappings, service,day);
				//saveMatrix();
				biCommunity.normalizeAdjMatrix();

				biCommunity.initLPA(mappings, indexedGroups.size());
				LPABipartite lpa = new LPABipartite(biCommunity.getmMatrix(), biCommunity.getcMatrixSum(), biCommunity.getcMatrixIntDegs(), biCommunity.getcMatrixExtDegs(), biCommunity.getBasePath());
				lpa.run(biCommunity.getmIntLabel(), biCommunity.getmExtLabel(), indexedGroups, biCommunity.getAsnList(), service,day);

				biCommunity.relabel();
				//saveBipartiteSummary();

				//MSGBipartite msg = new MSGBipartite(mMatrix, cMatrixSum);
				//msg.run(mIntLabel, mExtLabel);

				//relabel();
				//saveBipartiteSummary();

				biCommunity.calcScore();
			}
		}

	}

	private void read(HashMap<String, Integer> mappings, String service, String day) throws IOException {
		Set<String> intSet = CUtil.makeSet();// a hashset is returned, so no duplicates
		Set<String> extSet = CUtil.makeSet();

		String inPath = new String(basePath+day+"/features/ip/pair_services.features");
		BufferedReader in = new BufferedReader(new FileReader(inPath));
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			String src = split[2];
			String dest = split[3];
			if (ServiceParser.parse(src, dest).contains(service)) {
				if(mappings.get(split[0]) != null) {
					intSet.add(split[0]);
				}
				extSet.add(split[1]);//reading the internal and external IPs and loading them
			}

		}
		in.close();

		mIntIPList = new IndexedList<String>(intSet);
		mExtIPList = new IndexedList<String>(extSet);
		InputGroup inputGroup = new InputGroup(basePath);
		ExtIpToASNMap = inputGroup.readASNMap(mExtIPList,asnList);

		Log.log(Level.INFO, "External IPs = {0}", mExtIPList.size());
		Log.log(Level.INFO, "Internal IPs = {0}", mIntIPList.size());
		mMatrix = new Matrix<Double>(mIntIPList.size(), asnList.size(), 0.0);
		String inPath1 = new String(basePath+day+"/features/ip/pair_services.features");
		BufferedReader in1 = new BufferedReader(new FileReader(inPath1));
		String line1 = in1.readLine();
		while ((line1 = in1.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line1, ",");
			String src = split[2];
			String dest = split[3];
			double weight = Double.parseDouble(split[6]);
			if(!mappings.containsKey(split[0])) continue;

			int intIndex = mIntIPList.getIndex(split[0]);// instead of the Ips, the index of the Ips in the indexedList is used instead.
			Integer asnIndex = ExtIpToASNMap.get(mExtIPList.getIndex(split[1]));
			if(asnIndex == null) continue;

			if (ServiceParser.parse(src, dest).contains(service)) {
				mMatrix.set(intIndex, asnIndex, 1.0);
//					Double old = mMatrix.get(intIndex, asnIndex);
//					mMatrix.set(intIndex, asnIndex, old + weight); //many external Ips can have the same ASN number
			}
		}
		in1.close();
	}

//	private void saveMatrix() throws IOException {
//		BufferedWriter out = new BufferedWriter(new FileWriter(mCurrentOutPath + ".graph.csv"));
//		for (int i = 0; i < mIntIPList.size(); i++) {
//			for (int e : mMatrix.getNeighborsOfI(i)) {
//				String intIP = mIntIPList.get(i);
//				String extIP = mExtIPList.get(e);
//				out.write(intIP);
//				out.write(",");
//				out.write(extIP);
//				out.newLine();
//			}
//		}
//		out.close();
//	}

	private void normalizeAdjMatrix() {
		Log.log(Level.INFO, "Normalizing edge weights...");

//		NormalizeGaussian.normalizePos(mMatrix);
		mMatrix.updateTranspose();
		mMatrix.updateAdjacencyArrays();
	}

	private void initLPA(HashMap<String,Integer> mappings,int numOfSubnets) {
		mIntLabel = new int[mMatrix.getISize()];
		mExtLabel = new int[mMatrix.getJSize()];
		numOfSubnets++;

//		int c = 0;
//		for (int i = 0; i < mIntLabel.length; i++) {
//			mIntLabel[i] = c++;//randomly assign one unique label to each node
//		}

		for(String internalIp : mappings.keySet()){
			if(mIntIPList.getIndex(internalIp)!=-1){
				mIntLabel[mIntIPList.getIndex(internalIp)] = mappings.get(internalIp);//change the labels of those groups who have internal netreg
			}
		}

		//if(c<numOfSubnets){
		for(Integer asnNum : asnList.getList()){
			mExtLabel[asnList.getIndex(asnNum)] = numOfSubnets++;//randomly assign one unique label to each node
		}
//		}else{
//			for (int i = 0; i < mExtLabel.length; i++) {
//				mExtLabel[i] = c++;//randomly assign one unique label to each node
//			}
//		}

		cMatrixSum = MathUtil.sum(mMatrix);//sum of all weights
		cMatrixIntDegs = MathUtil.sumDimension(mMatrix, 2);// The sum of values in each column - Since this is the weighted version, this accounts for the degree
		cMatrixExtDegs = MathUtil.sumDimension(mMatrix, 1);// The sum of values in each row - Since this is the weighted version, this accounts for the degree
	}

	private void relabel() {
		Set<Integer> labels = CUtil.makeSet();
		for (int i = 0; i < mIntLabel.length; i++) {
			labels.add(mIntLabel[i]);
		}
		for (int i = 0; i < mExtLabel.length; i++) {
			labels.add(mExtLabel[i]);
		}

		mCommSize = labels.size();

		Log.log(Level.INFO, "Communities = {0}", mCommSize);

		IndexedList<Integer> labelList = new IndexedList<Integer>(labels);
		for (int i = 0; i < mIntLabel.length; i++) {
			mIntLabel[i] = labelList.getIndex(mIntLabel[i]);
		}
		for (int i = 0; i < mExtLabel.length; i++) {
			mExtLabel[i] = labelList.getIndex(mExtLabel[i]);
		}

		mAllIntLabels = new Histogram<Integer>();
		for (int i = 0; i < mIntLabel.length; i++) {
			mAllIntLabels.increment(mIntLabel[i]);
		}

		mAllExtLabels = new Histogram<Integer>();
		for (int i = 0; i < mExtLabel.length; i++) {
			mAllExtLabels.increment(mExtLabel[i]);
		}
	}

	private void  calcScore() {
		mExtFocusScore = new double[mExtLabel.length];

		mExtLabelRatio = CUtil.makeMap();
		for (Entry<Integer, Double> entry : mAllExtLabels.entrySet()) {
			Integer label = entry.getKey();
			double extCount = entry.getValue();
			double intCount = mAllIntLabels.get(label);
			mExtLabelRatio.put(label, extCount / intCount);
		}

		Histogram<Integer> neighborLabels = new Histogram<Integer>();
		for (int e = 0; e < mExtFocusScore.length; e++) {
			neighborLabels.clear();
			for (int i : mMatrix.getNeighborsOfJ(e)) {
				neighborLabels.increment(mIntLabel[i]);
			}

			for (Entry<Integer, Double> entry : neighborLabels.entrySet()) {
				int label = entry.getKey();
				mExtFocusScore[e] += entry.getValue() / mAllIntLabels.get(label);
			}

			mExtFocusScore[e] /= neighborLabels.size();
		}
	}

//	private void saveBipartiteSummary() throws IOException {
//		Matrix<Double> sumMatrix = new Matrix<Double>(mAllIntLabels.size(), mAllExtLabels.size(), 0.0);
//		for (int i = 0; i < mIntLabel.length; i++) {
//			for (int e : mMatrix.getNeighborsOfI(i)) {
//				int iLabel = mIntLabel[i];
//				int eLabel = mExtLabel[e];
//				double w = sumMatrix.get(iLabel, eLabel);
//				sumMatrix.set(iLabel, eLabel, w + 1.0);
//			}
//		}
//
//		GMLWriter.write(mCurrentOutPath + ".bicom.gml", sumMatrix, mAllIntLabels, mAllExtLabels);
//
//		BufferedWriter out = new BufferedWriter(new FileWriter(mCurrentOutPath + ".bicom.csv"));
//		for (int i = 0; i < mIntLabel.length; i++) {
//			out.write(mIntIPList.get(i));
//			out.write(",int");
//			out.write(String.valueOf(mIntLabel[i]));
//			out.newLine();
//		}
//		for (int e = 0; e < mExtLabel.length; e++) {
//			out.write(mExtIPList.get(e));
//			out.write(",ext");
//			out.write(String.valueOf(mExtLabel[e]));
//			out.newLine();
//		}
//		out.close();
//	}

}
