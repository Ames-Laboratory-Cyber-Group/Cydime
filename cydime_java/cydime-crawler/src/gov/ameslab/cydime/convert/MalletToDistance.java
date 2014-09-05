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

package gov.ameslab.cydime.convert;

import gov.ameslab.cydime.convert.metric.EuclideanDistance;
import gov.ameslab.cydime.convert.metric.Jaccard;
import gov.ameslab.cydime.convert.metric.JensenShannon;
import gov.ameslab.cydime.convert.metric.KullbackLeibler;
import gov.ameslab.cydime.convert.metric.ManhattanDistance;
import gov.ameslab.cydime.convert.metric.Pearson;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.Histogram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Metric;
import cc.mallet.types.NormalizedDotProductMetric;

public class MalletToDistance {

	public static void main(String[] args) throws IOException {
		Set<String> missionURLs = CUtil.makeSet(FileUtils.readLines(new File(Config.INSTANCE.getString(Config.MISSION_DOM_FILE))));
		InstanceList instances = InstanceList.load(new File(Config.INSTANCE.getString(Config.MALLET_FILE)));
		List<FeatureVector> missionVectors = readMissionVectors(missionURLs, instances);
		if (missionVectors.isEmpty()) {
			throw new RuntimeException("Error: no mission vectors found.");
		}
		
		int FEATURE_SIZE = missionVectors.get(0).getAlphabet().size();
		
		List<Histogram<String>> dists = CUtil.makeList();
		
		//Metrics on raw vectors
		computeDistances(dists, missionURLs, instances, missionVectors,
				new NormalizedDotProductMetric(),	//Cosine
				new ManhattanDistance(),	//Manhattan
				new EuclideanDistance(),	//Euclidean
				new Jaccard(),				//Jaccard
				new Pearson(FEATURE_SIZE)	//Pearson Correlation
				);
		
		//Normalize
		for (FeatureVector v : missionVectors) {
			normalize(v);
		}
		for (Instance i : instances) {
			FeatureVector v = (FeatureVector) i.getData();
			normalize(v);
		}
		
		//Metrics on normalized vectors
		computeDistances(dists, missionURLs, instances, missionVectors,
				new KullbackLeibler(FEATURE_SIZE),	//Averaged Kullback-Leibler Divergence
				new JensenShannon(FEATURE_SIZE)		//Jensen Shannon Divergence
				);
		
		writeResult(dists);		
	}

	private static void computeDistances(List<Histogram<String>> dists, Set<String> missionURLs, InstanceList instances, List<FeatureVector> missionVectors, Metric ... metrics) {
		for (int m = 0; m < metrics.length; m++) {
			Histogram<String> dist = new Histogram<String>();
			for (FeatureVector missionVector : missionVectors) {
				for (Instance i : instances) {
					String domain = getDomain(i);
					if (missionURLs.contains(domain)) continue;
					
					FeatureVector iVector = (FeatureVector) i.getData();
					double d = metrics[m].distance(missionVector, iVector);
					dist.min(domain, d);
				}
			}
			
			dist.normalizeTo(0.0, 1.0);
			dist.replaceNanWith(1.0);
			dist.replaceInfiniteWith(1.0);
			dists.add(dist);
		}
	}

	private static void normalize(FeatureVector v) {
		double oneNorm = v.oneNorm();
		v.timesEquals(1 / oneNorm);
	}

	private static List<FeatureVector> readMissionVectors(Set<String> missionURLs, InstanceList instances) throws IOException {
		List<FeatureVector> missionVectors = CUtil.makeList();
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			String domain = getDomain(inst);
			if (missionURLs.contains(domain)) {
				missionVectors.add((FeatureVector) inst.getData());
			}
		}

		System.out.println("Read mission vectors " + missionVectors.size() + "/" + missionURLs.size() + ".");				
		return missionVectors;
	}

	private static String getDomain(Instance i) {
		String name = i.getName().toString();
		int lastIndexOf = name.lastIndexOf("/");
		return name.substring(lastIndexOf + 1);
	}
	
	private static void writeResult(List<Histogram<String>> dists) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getPath(Config.NEW_LEXICAL_MISSION_SIM_FILE)));
		for (String name : dists.get(0).getSortedKeysByValue()) {
			out.write(name);
			for (Histogram<String> dist : dists) {
				out.write(",");
				out.write(String.valueOf(1.0 - dist.get(name)));
			}
			out.newLine();
		}
		out.close();
	}

}
