package gov.ameslab.cydime.ranker.evaluate;

import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.math3.stat.StatUtils;

public class RankerResult {

	private double[][] mResult;
	private double[] mMean;
	private double[] mStd;
	
	public RankerResult(int evaluatorSize, int runSize) {
		mResult = new double[evaluatorSize][runSize];
		mMean = new double[evaluatorSize];
		mStd = new double[evaluatorSize];
	}

	public void addRun(int run, String scoreFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(scoreFile));
		String line = null;
		int e = 0;
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			double score = Double.parseDouble(split[1]);
			mResult[e][run] = score;
			e++;
		}
		in.close();		
	}

	public void computeSummary() {
		for (int i = 0; i < mResult.length; i++) {
			mMean[i] = StatUtils.mean(mResult[i]);
			mStd[i] = Math.sqrt(StatUtils.variance(mResult[i]));
		}
	}

	public double getMean(int e) {
		return mMean[e];
	}
	
	public double getStd(int e) {
		return mStd[e];
	}
	
	public double[] getResult(int e) {
		return mResult[e];
	}
	
}
