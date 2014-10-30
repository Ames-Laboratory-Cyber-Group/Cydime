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

package gov.ameslab.cydime.cluster;

import gov.ameslab.cydime.cluster.lpaplus.LPABipartite;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.MapSet;
import gov.ameslab.cydime.util.WeightedGraph;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CydimeClusterExp {

	private static final Logger Log = Logger.getLogger(CydimeClusterExp.class.getName());

	private static final double EDGE_THRESHOLD = 0.01;
	
	private Algorithm mAlgorithm;
	private String mDataset;
	private double mParam;
	private int mIteration;

	private WeightedGraph<String> mSourceGraph;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			printUsage();
			return;
		}

		new CydimeClusterExp(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] ALGO DATASET PARAM ITER");
		System.out.println("    ALGO: [biclique|modularity]");
		System.out.println("    DATASET: [byte|days]");
		System.out.println("    PARAM: parameter");
		System.out.println("    ITER: iteration");
	}

	public CydimeClusterExp(String[] args) {
		if (args[0].equalsIgnoreCase("biclique")) {
			mAlgorithm = null;
		} else if (args[0].equalsIgnoreCase("modularity")) {
			mAlgorithm = new LPABipartite();
		}

		mDataset = args[1];
		mParam = Double.parseDouble(args[2]);
		mIteration = Integer.parseInt(args[3]);
	}

	private void run() throws Exception {
		Log.log(Level.INFO, "Reading source graph...");
		
		mSourceGraph = WeightedGraph.readCSV(getGraphFile(-1));
		
		writeFirstGraph();
		for (int i = 0; i < mIteration; i++) {
			Log.log(Level.INFO, "Running iteration {0}...", i);
			
			String prevMapSourceFile = getMapSourceFile(i - 1);
			String graphFile = getGraphFile(i);
			String mapFile = getMapFile(i);
			String mapSourceFile = getMapSourceFile(i);
			String nextGraphFile = getGraphFile(i + 1);
			
			Dataset data = mAlgorithm.run(graphFile, mParam);
			
			data.writeMap(mapFile, i);
			
			writeMapSource(prevMapSourceFile, mapFile, mapSourceFile);
			
			writeNextGraph(mapSourceFile, nextGraphFile);
			
			writeGML(nextGraphFile);
		}
	}

	private void writeFirstGraph() throws IOException {
		WeightedGraph<String> g = WeightedGraph.readCSV(getGraphFile(-1));
		g.log();
		g.rescale();
		g.writeCSV(getGraphFile(0), EDGE_THRESHOLD);
	}

	private void writeMapSource(String prevMapSourceFile, String mapFile, String mapSourceFile) throws IOException {
		if (prevMapSourceFile == null) {
			FileUtil.copy(mapFile, mapSourceFile);
		} else {
			MapSet<String, String> prevMapSource = MapSet.readCSV(prevMapSourceFile);
			MapSet<String, String> map = MapSet.readCSV(mapFile);
			MapSet<String, String> mapSource = prevMapSource.compose(map);
			mapSource.writeCSV(mapSourceFile);
		}
	}

	private void writeNextGraph(String mapSourceFile, String graphFile) throws IOException {
		MapSet<String, String> mapSource = MapSet.readCSV(mapSourceFile);
		WeightedGraph<String> g = new WeightedGraph<String>();
		
		for (Entry<String, Map<String, Double>> intEntry : mSourceGraph.entrySet()) {
			String intID = intEntry.getKey();
			for (Entry<String, Double> extEntry : intEntry.getValue().entrySet()) {
				String extID = extEntry.getKey();
				double w = extEntry.getValue();

				for (String intMap : mapSource.get(intID)) {
					String intCluster = getCluster(intMap);
					for (String extMap : mapSource.get(extID)) {
						String extCluster = getCluster(extMap);
						
						//Self edge gets automatic edge at the end and is not counted during normalization
						if (intCluster.equals(extCluster)) continue;
						
						g.add(intMap, extMap, w);
					}
				}
			}
		}
		
		g.log();
		g.rescale();
		
		addSelfEdge(g);
		
		g.writeCSV(graphFile, EDGE_THRESHOLD);
	}

	private String getCluster(String map) {
		int i = map.indexOf("_");
		return map.substring(i + 1);
	}

	private void addSelfEdge(WeightedGraph<String> g) {
		for (Entry<String, Map<String, Double>> intEntry : g.entrySet()) {
			String intMap = intEntry.getKey();
			String intCluster = getCluster(intMap);			
			for (Entry<String, Double> extEntry : intEntry.getValue().entrySet()) {
				String extMap = extEntry.getKey();
				String extCluster = getCluster(extMap);
				
				if (intCluster.equals(extCluster)) {
					g.set(intMap, extMap, 1.0);
				}				
			}
		}
	}
	
	private void writeGML(String graphFile) {
		// TODO Auto-generated method stub
		
	}

	private String getMapFile(int it) {
		return ensurePath(getRootPath() + "cluster" + it) + "/" + mDataset + "_all_map.csv";
	}

	private String getMapSourceFile(int it) {
		if (it < 0) {
			return null;
		} else {
			return ensurePath(getRootPath() + "cluster" + it) + "/" + mDataset + "_all_mapsrc.csv";
		}
	}

	private String getGraphFile(int it) {
		if (it < 0) {
			return Config.BIGRAPH_PATH + mDataset + "_all.csv";
		} else {
			return ensurePath(getRootPath() + "bigraph" + it) + "/" + mDataset + "_all.csv";
		}
	}

	private String getRootPath() {
		DecimalFormat f = new DecimalFormat("0000");
		int param = (int) (mParam * 1000);
		return mAlgorithm.getName() + "_" + f.format(param) + "/";
	}

	private String ensurePath(String path) {
		File p = new File(path);
		if (!p.exists()) {
			p.mkdirs();
		}
		return path;
	}

}
