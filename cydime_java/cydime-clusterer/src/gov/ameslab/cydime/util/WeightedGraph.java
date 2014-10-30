package gov.ameslab.cydime.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WeightedGraph<T> {
	
	private Map<T, Map<T, Double>> mGraph;
	
	public WeightedGraph() {
		mGraph = CUtil.makeMap();
	}
	
	public Set<Entry<T, Map<T, Double>>> entrySet() {
		return mGraph.entrySet();
	}

	public void set(T src, T tar, double w) {
		Map<T, Double> srcMap = mGraph.get(src);
		if (srcMap == null) {
			srcMap = CUtil.makeMap();
			mGraph.put(src, srcMap);
		}
		
		srcMap.put(tar, w);
	}

	public void add(T src, T tar, double w) {
		Map<T, Double> srcMap = mGraph.get(src);
		if (srcMap == null) {
			srcMap = CUtil.makeMap();
			mGraph.put(src, srcMap);
		}
		
		Double oldW = srcMap.get(tar);
		if (oldW == null) {
			oldW = 0.0;
		}
		
		srcMap.put(tar, oldW + w);
	}

	public void log() {
		for (T i : CUtil.makeSet(mGraph.keySet())) {
			Map<T, Double> eMap = mGraph.get(i);
			for (T e : CUtil.makeSet(eMap.keySet())) {
				double v = eMap.get(e);
				eMap.put(e, Math.log(v + 1.0));
			}
		}
	}

	public void rescale() {
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		
		for (Entry<T, Map<T, Double>> intEntry : mGraph.entrySet()) {
			for (Entry<T, Double> extEntry : intEntry.getValue().entrySet()) {
				double v = extEntry.getValue();
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
		}
		
		double range = max - min;
		if (range <= 0.0) return;
		
		for (T i : CUtil.makeSet(mGraph.keySet())) {
			Map<T, Double> eMap = mGraph.get(i);
			for (T e : CUtil.makeSet(eMap.keySet())) {
				double v = eMap.get(e);
				eMap.put(e, (v - min) / range);
			}
		}
	}

	public static WeightedGraph<String> readCSV(String file) throws IOException {
		WeightedGraph<String> g = new WeightedGraph<String>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			double w = Double.parseDouble(split[2]);
			g.add(split[0], split[1], w);
		}
		in.close();		
		return g;
	}

	public void writeCSV(String file) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		for (Entry<T, Map<T, Double>> intEntry : mGraph.entrySet()) {
			T intID = intEntry.getKey();
			for (Entry<T, Double> extEntry : intEntry.getValue().entrySet()) {
				T extID = extEntry.getKey();
				out.write(intID.toString());
				out.write(",");
				out.write(extID.toString());
				out.write(",");
				out.write(String.valueOf(extEntry.getValue()));
				out.newLine();
			}
		}		
		out.close();
	}

	public void writeCSV(String file, double edgeThreshold) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		for (Entry<T, Map<T, Double>> intEntry : mGraph.entrySet()) {
			T intID = intEntry.getKey();
			for (Entry<T, Double> extEntry : intEntry.getValue().entrySet()) {
				T extID = extEntry.getKey();
				double w = extEntry.getValue();
				if (w < edgeThreshold) continue;
				
				out.write(intID.toString());
				out.write(",");
				out.write(extID.toString());
				out.newLine();
			}
		}		
		out.close();
	}

}
