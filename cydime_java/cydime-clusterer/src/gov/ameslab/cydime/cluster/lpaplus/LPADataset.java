package gov.ameslab.cydime.cluster.lpaplus;

import gov.ameslab.cydime.cluster.Dataset;
import gov.ameslab.cydime.preprocess.Matrix;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.MathUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class LPADataset implements Dataset {

	public IndexedList<String> mIntList;
	public IndexedList<String> mExtList;
	public Matrix<Double> mMatrixIntExt; // mMatrix[internal][external]
	public int[] mIntLabel;
	public int[] mExtLabel;
	
	public double cMatrixSum;
	public double[] cIntDegs;
	public double[] cExtDegs;
	
	public LPADataset() {
	}
	
	public static LPADataset load(String graphFile) throws IOException {
		LPADataset data = new LPADataset();
		
		Set<String> intSet = CUtil.makeSet();
		Set<String> extSet = CUtil.makeSet();
		
		BufferedReader in = new BufferedReader(new FileReader(graphFile));
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");			
			intSet.add(split[0]);
			extSet.add(split[1]);
		}
		in.close();
	
		data.mIntList = new IndexedList<String>(intSet);
		data.mExtList = new IndexedList<String>(extSet);
		
		data.mMatrixIntExt = new Matrix<Double>(data.mIntList.size(), data.mExtList.size(), 0.0);
		in = new BufferedReader(new FileReader(graphFile));
		line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			
			int intIndex = data.mIntList.getIndex(split[0]);
			int extIndex = data.mExtList.getIndex(split[1]);
			//double weight = Double.parseDouble(split[2]);
			double weight = 1.0;
			Double old = data.mMatrixIntExt.get(intIndex, extIndex);
			data.mMatrixIntExt.set(intIndex, extIndex, old + weight);
		}
		in.close();
		
		data.mMatrixIntExt.updateTranspose();
		data.mMatrixIntExt.updateAdjacencyArrays();
		
		data.mIntLabel = new int[data.mMatrixIntExt.getISize()];
		data.mExtLabel = new int[data.mMatrixIntExt.getJSize()];
		Arrays.fill(data.mIntLabel, -1);
		Arrays.fill(data.mExtLabel, -1);
		
		data.cMatrixSum = MathUtil.sum(data.mMatrixIntExt);
		data.cIntDegs = MathUtil.sumDimension(data.mMatrixIntExt, 2);
		data.cExtDegs = MathUtil.sumDimension(data.mMatrixIntExt, 1);
		
		return data;
	}
	
	public void writeMap(String mapFile, int it) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(mapFile));
		String prefix = "int" + it + "_";
		for (int i = 0; i < mIntLabel.length; i++) {
			if (mIntLabel[i] < 0) continue;
			
			String intID = mIntList.get(i);
			out.write(intID);
			out.write(",");
			out.write(prefix);
			out.write(String.valueOf(mIntLabel[i]));
			out.newLine();
		}
		
		prefix = "ext" + it + "_";
		for (int i = 0; i < mExtLabel.length; i++) {
			if (mExtLabel[i] < 0) continue;
			
			String extID = mExtList.get(i);
			out.write(extID);
			out.write(",");
			out.write(prefix);
			out.write(String.valueOf(mExtLabel[i]));
			out.newLine();
		}
		out.close();		
	}

}
