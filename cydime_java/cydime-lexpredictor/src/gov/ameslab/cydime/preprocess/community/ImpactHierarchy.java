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

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.community.mroc.Cluster;
import gov.ameslab.cydime.preprocess.community.mroc.MROC;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Histogram;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.MapSet;
import gov.ameslab.cydime.util.MathUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class ImpactHierarchy extends FeatureSet {
	
	private static final Logger Log = Logger.getLogger(ImpactHierarchy.class.getName());
	
	private static final int MIN_DAYS = 14;
	private static final double NEAL_ALPHA = 0.00001;
	
	private Set<String> mNetreg;
	private Map<String, String> mASNNameMap;
	private MapSet<String, String> mNetregClusters;
	private MapSet<String, Integer> mIntClusters;
	
	private IndexedList<String> mExtASNList;
	private IndexedList<String> mIntIPList;
	private Matrix<Integer> mASNIPMatrix; // mMatrix[asn][ip]
	
	private LogFact mLogFact;
	private Graph<Integer, WeightedEdge> mASNGraph;
	
	private ImpactHierarchy() {}
	
	public ImpactHierarchy(List<String> ids, String inPath, String outPath) {
		super(ids, inPath, outPath);
	}

	public void run() throws IOException {
		Log.log(Level.INFO, "Processing InfluenceGraph...");
		
		readNetreg();
		readASN();
//		read();
		readTmp();
		reduce();
		save();
		mroc();
	}
	
	private void readASN() throws IOException {
		mASNNameMap = CUtil.makeMap();
		BufferedReader in = new BufferedReader(new FileReader("ipASNMap.csv"));
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			if (split.length < 3) continue;
			
			mASNNameMap.put(split[1], split[2]);
		}
		in.close();
	}

	private void readNetreg() throws IOException {
		Log.log(Level.INFO, "Reading Netreg...");
		mNetreg = CUtil.makeSet();
		mNetregClusters = new MapSet<String, String>();
		BufferedReader in = new BufferedReader(new FileReader("netreg_nobuilding.csv"));
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			mNetreg.add(split[0]);
			mNetregClusters.add(split[1], split[0]);
			mNetregClusters.add(split[2], split[0]);
			mNetregClusters.add(split[1] + " * " + split[2], split[0]);
		}
		in.close();
	}

	private void read() throws IOException {
		Map<String, MapSet<String, String>> asnIPDaySet = CUtil.makeMap();		
		for (String inPath : mFeaturePaths) {
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				MapSet<String, String> ipDaySet = asnIPDaySet.get(split[1]);
				if (ipDaySet == null) {
					ipDaySet = new MapSet<String, String>();
					asnIPDaySet.put(split[1], ipDaySet);
				}
				ipDaySet.add(split[0], inPath);
			}
			in.close();
		}
		
		Set<String> extSet = CUtil.makeSet();
		Set<String> intSet = CUtil.makeSet();
		for (Entry<String, MapSet<String, String>> entry : asnIPDaySet.entrySet()) {
			String asn = entry.getKey();
			MapSet<String, String> ipDaySet = entry.getValue();
			boolean isEmpty = true;
			
			for (String ip : ipDaySet.keySet()) {
				int days = ipDaySet.get(ip).size();
				if (days >= MIN_DAYS) {
					intSet.add(ip);
					isEmpty = false;
				}
			}
			
			if (!isEmpty) {
				extSet.add(asn);
			}
		}
		
		mExtASNList = new IndexedList<String>(extSet);
		mIntIPList = new IndexedList<String>(intSet);
		
		Log.log(Level.INFO, "External ASNs = {0}", mExtASNList.size());
		Log.log(Level.INFO, "Internal IPs = {0}", mIntIPList.size());
		
		int edges = 0;
		mASNIPMatrix = new Matrix<Integer>(mExtASNList.size(), mIntIPList.size(), 0);
		for (Entry<String, MapSet<String, String>> entry : asnIPDaySet.entrySet()) {
			String asn = entry.getKey();
			int extIndex = mExtASNList.getIndex(asn);
			MapSet<String, String> ipDaySet = entry.getValue();
			
			for (String ip : ipDaySet.keySet()) {
				int intIndex = mIntIPList.getIndex(ip);
				int days = ipDaySet.get(ip).size();
				if (days >= MIN_DAYS) {
					mASNIPMatrix.set(extIndex, intIndex, days);
					edges++;
				}
			}
		}
		
		mASNIPMatrix.updateTranspose();
		mASNIPMatrix.updateAdjacencyArrays();
		
		Log.log(Level.INFO, "Edges = {0}", edges);
	}

	private void readTmp() throws IOException {
		Map<String, MapSet<String, String>> asnIPDaySet = CUtil.makeMap();		
		BufferedReader in = new BufferedReader(new FileReader("ipASN.csv"));
		String line;
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			if (!mNetreg.contains(split[0])) continue;
			
			MapSet<String, String> ipDaySet = asnIPDaySet.get(split[1]);
			if (ipDaySet == null) {
				ipDaySet = new MapSet<String, String>();
				asnIPDaySet.put(split[1], ipDaySet);
			}
			ipDaySet.add(split[0], split[2]);
		}
		in.close();
	
		Set<String> extSet = CUtil.makeSet();
		Set<String> intSet = CUtil.makeSet();
		for (Entry<String, MapSet<String, String>> entry : asnIPDaySet.entrySet()) {
			String asn = entry.getKey();
			MapSet<String, String> ipDaySet = entry.getValue();
			boolean isEmpty = true;
			
			for (String ip : ipDaySet.keySet()) {
				int days = ipDaySet.get(ip).size();
				if (days >= MIN_DAYS) {
					intSet.add(ip);
					isEmpty = false;
				}
			}
			
			if (!isEmpty) {
				extSet.add(asn);
			}
		}
		
		mExtASNList = new IndexedList<String>(extSet);
		mIntIPList = new IndexedList<String>(intSet);
		
		Log.log(Level.INFO, "External ASNs = {0}", mExtASNList.size());
		Log.log(Level.INFO, "Internal IPs = {0}", mIntIPList.size());
		
		int edges = 0;
		mASNIPMatrix = new Matrix<Integer>(mExtASNList.size(), mIntIPList.size(), 0);
		for (Entry<String, MapSet<String, String>> entry : asnIPDaySet.entrySet()) {
			String asn = entry.getKey();
			int extIndex = mExtASNList.getIndex(asn);
			MapSet<String, String> ipDaySet = entry.getValue();
			
			for (String ip : ipDaySet.keySet()) {
				int intIndex = mIntIPList.getIndex(ip);
				int days = ipDaySet.get(ip).size();
				if (days >= MIN_DAYS) {
					mASNIPMatrix.set(extIndex, intIndex, days);
					edges++;
				}
			}
		}
		
		mASNIPMatrix.updateTranspose();
		mASNIPMatrix.updateAdjacencyArrays();
		
		Log.log(Level.INFO, "Edges = {0}", edges);
	}
	
	private void reduce() {
		Log.log(Level.INFO, "Reducing ASN graph...");
		
		mIntClusters = new MapSet<String, Integer>();
		for (String key : mNetregClusters.keySet()) {
			for (String ip : mNetregClusters.get(key)) {
				int index = mIntIPList.getIndex(ip);
				if (index < 0) continue;
				
				mIntClusters.add(key, index);
			}
		}
		
		mLogFact = new LogFact();
		mASNGraph = new UndirectedSparseGraph<Integer, WeightedEdge>();
		for (int i = 0; i < mExtASNList.size(); i++) {
			mASNGraph.addVertex(i);
		}
				
		for (int i1 = 0; i1 < mExtASNList.size() - 1; i1++) {
			Set<Integer> j1s = mASNIPMatrix.getNeighborsOfI(i1);
			for (int i2 : find2Hops(i1)) {
				if (i2 <= i1) continue;
				
				int[] j2s = mASNIPMatrix.getNeighborListOfI(i2);
				int intersection = 0;
				for (int i = 0; i < j2s.length; i++) {
					if (j1s.contains(j2s[i])) {
						intersection++;
					}
				}
				
				double nealProb = getNealProb(mIntIPList.size(), j1s.size(), j2s.length, intersection);
//				if (nealProb > 0.0)
//					System.out.println(Math.log(nealProb));
				
				if (nealProb < NEAL_ALPHA) {
					mASNGraph.addEdge(new WeightedEdge(1.0 - nealProb), i1, i2);
					
//					System.out.println(j1s.size() + " " + j2s.length + " " + intersection + " : " + nealProb);
				}
			}
		}
		
		int connectedVertices = 0;
		for (Integer i :mASNGraph.getVertices()) {
			if (mASNGraph.degree(i) > 0) {
				connectedVertices++;
			}
		}
		
		Log.log(Level.INFO, "Vertices = {0}", connectedVertices);
		Log.log(Level.INFO, "Edges = {0}", mASNGraph.getEdgeCount());
		
		List<WeightedEdge> edges = CUtil.makeList(mASNGraph.getEdges());
		if (edges.size() > connectedVertices * 3) {
			Collections.sort(edges, new Comparator<WeightedEdge>() {
	
				@Override
				public int compare(WeightedEdge o1, WeightedEdge o2) {
					return Double.compare(o1.Weight, o2.Weight);
				}
				
			});
			
			int removeLength = edges.size() - connectedVertices * 3;
			for (int i = 0; i < removeLength; i++) {
				mASNGraph.removeEdge(edges.get(i));
			}
		}
		
		Log.log(Level.INFO, "Post-pruned Edges = {0}", mASNGraph.getEdgeCount());
	}

	private Set<Integer> find2Hops(int i1) {
		Set<Integer> set = CUtil.makeSet();
		int[] j1s = mASNIPMatrix.getNeighborListOfI(i1);
		for (int j1 = 0; j1 < j1s.length; j1++) {
			int[] i2s = mASNIPMatrix.getNeighborListOfJ(j1s[j1]);
			for (int i2 = 0; i2 < i2s.length; i2++) {
				set.add(i2s[i2]);
			}
		}
		return set;
	}
	
	private double[] cLogs = new double[1000];

	private double getNealProb(int A, int Di, int Dj, int Pij) {
		if (Pij == 0) return 1.0;
		
		int Dmin = Math.min(Di, Dj);
		int Dmax = Math.max(Di, Dj);
		
		if (Pij > Dmin / 2) {
			int length = Dmin - Pij + 1;
			if (cLogs.length < length) {
				cLogs = new double[length];
			}
			
			for (int x = 0; x < length; x++) {
				cLogs[x] = getNealProbLogX(A, Dmin, Dmax, Dmin - x);
			}
			
			double logsum = MathUtil.sumLog(cLogs, 0, length);
			return Math.exp(logsum);
		} else {
			int end = Pij;
			int begin = Math.max(0, Di + Dj - A);
			int length = end - begin;
			if (cLogs.length < length) {
				cLogs = new double[length];
			}
			
			for (int x = begin; x < end; x++) {
				cLogs[x] = getNealProbLogX(A, Dmin, Dmax, x);
			}
			
			double logsum = MathUtil.sumLog(cLogs, begin, end);
			return 1.0 - Math.exp(logsum);
		}
	}

	private double getNealProbLogX(int A, int Dmin, int Dmax, int x) {
		double logsum = mLogFact.logChoose(Dmin, x);
		logsum += mLogFact.logChoose(A - Dmin, Dmax - x);
		logsum -= mLogFact.logChoose(A, Dmax);
		return logsum;
	}

	private void save() throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter("asnGraph.csv"));
		for (WeightedEdge e : mASNGraph.getEdges()) {
			Pair<Integer> endpoints = mASNGraph.getEndpoints(e);
			out.write(mExtASNList.get(endpoints.getFirst()));
			out.write(",");
			out.write(mExtASNList.get(endpoints.getSecond()));
			out.newLine();
		}		
		out.close();
		
		out = new BufferedWriter(new FileWriter("asnGraph.gml"));
		out.write("graph");
		out.newLine();
		out.write("[");
		out.newLine();
		for (int v : mASNGraph.getVertices()) {
			out.write("node");
			out.newLine();
			out.write("[");
			out.newLine();
			out.write("id  \"" + mExtASNList.get(v) + "\"");
			out.newLine();
			out.write("label \"" + mASNNameMap.get(mExtASNList.get(v)) + "\"");
			out.newLine();
			out.write("]");
			out.newLine();
		}		
		for (WeightedEdge e : mASNGraph.getEdges()) {
			out.write("edge");
			out.newLine();
			out.write("[");
			out.newLine();
			Pair<Integer> endpoints = mASNGraph.getEndpoints(e);
			out.write("source \"" + mExtASNList.get(endpoints.getFirst()) + "\"");
			out.newLine();
			out.write("target \"" + mExtASNList.get(endpoints.getSecond()) + "\"");
			out.newLine();
			out.write("]");
			out.newLine();
		}		
		out.write("]");
		out.close();
	}
	
	
	private static class Matching implements Comparable<Matching> {
		
		public double Score;
		public Cluster<Integer> ExtCluster;
		private String IntKey;
		public Set<Integer> IntCluster;
		
		public Matching(double score, Cluster<Integer> e, String iKey, Set<Integer> i) {
			set(score, e, iKey, i);
		}

		public void set(double score, Cluster<Integer> e, String iKey, Set<Integer> i) {
			Score = score;
			ExtCluster = e;
			IntKey = iKey;
			IntCluster = i;
		}
		
		@Override
		public int compareTo(Matching o) {
			return Double.compare(Score, o.Score);
		}

	}
	
//	private void mroc() throws IOException {
//		Log.log(Level.INFO, "Clustering MROC...");
//		
//		MROC mroc = new MROC();
//		DirectedGraph<Cluster<Integer>, Edge> forest = mroc.run(mASNGraph);
//		
//		Map<Cluster<Integer>, Set<Integer>> projectedMembers = CUtil.makeMap();
//		for (Cluster<Integer> v : forest.getVertices()) {
//			projectedMembers.put(v, project(v.getMembers()));
//		}
//		
//		Log.log(Level.INFO, "Cluster Matching...");
//		
//		List<Matching> matchings = CUtil.makeList();
//		for (String key : mIntClusters.keySet()) {
//			Set<Integer> intCluster = mIntClusters.get(key);
//			Matching best = new Matching(0.0, null, null, null);
//			
//			for (Entry<Cluster<Integer>, Set<Integer>> entry : projectedMembers.entrySet()) {
//				double jaccard = getJaccard(intCluster, entry.getValue());
//				if (Double.isNaN(jaccard) || jaccard <= 0.0) continue;
//				if (jaccard > best.Score) {
//					best.set(jaccard, entry.getKey(), key, intCluster);
//				}
//			}
//			
//			matchings.add(best);
//		}
//
//		Collections.sort(matchings);
//		Collections.reverse(matchings);
//		
//		DecimalFormat f = new DecimalFormat("0%");
//		BufferedWriter out = new BufferedWriter(new FileWriter("asnMatch.csv"));		
//		for (Matching m : matchings) {
//			out.write("Internal Label = " + m.IntKey);
//			out.newLine();
//			out.write("Match = " + f.format(m.Score));
//			out.newLine();
//			
//			for (int a : m.ExtCluster.getMembers()) {
//				String asn = mExtASNList.get(a);
//				String asnName = mASNNameMap.get(asn);
//				out.write(asnName);
//				out.newLine();
//			}
//			
//			for (int i : m.IntCluster) {
//				String ip = mIntIPList.get(i);
//				out.write(ip);
//				out.newLine();
//			}
//			
//			out.newLine();
//		}
//		
//		out.close();
//	}
	
	private void mroc() throws IOException {
		Log.log(Level.INFO, "Clustering MROC...");
		
		MROC mroc = new MROC();
		DirectedGraph<Cluster<Integer>, Edge> forest = mroc.run(mASNGraph);
		
		Map<Cluster<Integer>, Histogram<Integer>> projectedMembers = CUtil.makeMap();
		for (Cluster<Integer> v : forest.getVertices()) {
			projectedMembers.put(v, project(v.getMembers()));
		}
		
		Log.log(Level.INFO, "Cluster Matching...");
		
		List<Matching> matchings = CUtil.makeList();
		for (String key : mIntClusters.keySet()) {
			Set<Integer> intCluster = mIntClusters.get(key);
			Histogram<Integer> intHist = new Histogram<Integer>(); 
			for (Integer i : intCluster) {
				intHist.increment(i);
			}
			intHist.normalize();
			
			Matching best = new Matching(0.0, null, null, null);
			
			for (Entry<Cluster<Integer>, Histogram<Integer>> entry : projectedMembers.entrySet()) {
				double score = getSimilarity(intHist, entry.getValue());
				if (Double.isNaN(score) || score <= 0.0) continue;
				if (score > best.Score) {
					best.set(score, entry.getKey(), key, intCluster);
				}
			}
			
			matchings.add(best);
		}

		Collections.sort(matchings);
		Collections.reverse(matchings);
		
		DecimalFormat f = new DecimalFormat("0%");
		BufferedWriter out = new BufferedWriter(new FileWriter("asnMatchJS.csv"));		
		for (Matching m : matchings) {
			out.write("Internal Label = " + m.IntKey);
			out.newLine();
			out.write("Match = " + f.format(m.Score));
			out.newLine();
			
			for (int a : m.ExtCluster.getMembers()) {
				String asn = mExtASNList.get(a);
				String asnName = mASNNameMap.get(asn);
				out.write(asnName);
				out.newLine();
			}
			
			for (int i : m.IntCluster) {
				String ip = mIntIPList.get(i);
				out.write(ip);
				out.newLine();
			}
			
			out.newLine();
		}
		
		out.close();
	}
	
//	private static <T> double getSimilarity(Set<T> c1, Set<T> c2) {
//		Set<T> and = CUtil.makeSet();
//		and.addAll(c1);
//		and.retainAll(c2);
//		
//		int orSize = c1.size() + c2.size() - and.size();		
//		if (orSize == 0) return Double.NaN;
//		return 1.0 * and.size() / orSize;
//	}
	
//	private Set<Integer> project(Set<Integer> extMembers) {
//		Set<Integer> intMembers = CUtil.makeSet();
//		for (int i : extMembers) {
//			Set<Integer> js = mASNIPMatrix.getNeighborsOfI(i);
//			intMembers.addAll(js);
//		}
//		return intMembers;
//	}
	
	private static double getSimilarity(Histogram<Integer> h1, Histogram<Integer> h2) {
		Histogram<Integer> m = new Histogram<Integer>();
		m.add(h1);
		m.add(h2);
		m.divide(2.0);
		
		return 1.0 - (getJS(h1, m) + getJS(h2, m)) / 2.0;
	}
	
	private static double getJS(Histogram<Integer> p, Histogram<Integer> m) {
		double result = 0.0;
		for (Entry<Integer, Double> entry : m.entrySet()) {
			double pV = p.get(entry.getKey());
			double mV = entry.getValue();
			if (pV <= 0.0) continue;
			
			result += pV * (Math.log(pV) - Math.log(mV));
		}
		return result;
	}

	private Histogram<Integer> project(Set<Integer> extMembers) {
		Histogram<Integer> intMembers = new Histogram<Integer>();
		for (int i : extMembers) {
			for (int j : mASNIPMatrix.getNeighborsOfI(i)) {
				intMembers.increment(j);
			}
		}
		intMembers.normalize();
		return intMembers;
	}

	
	public static void main(String[] args) throws IOException {
		ImpactHierarchy g = new ImpactHierarchy();
		g.run();
	}
	
}
