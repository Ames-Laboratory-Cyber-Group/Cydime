package gov.ameslab.cydime.ranker.evaluate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.stat.inference.TTest;

public class RankerResultList {

	private static final double ALPHA = 0.05;
	
	private RankerResult[] mResults;
	private int[] mMaxIndex;
	private boolean[][] mTTestRejected;
	
	public RankerResultList(int algSize, int evaluatorSize, int runSize) {
		mResults = new RankerResult[algSize];
		mMaxIndex = new int[evaluatorSize];
		mTTestRejected = new boolean[algSize][evaluatorSize];
		
		for (int i = 0; i < mResults.length; i++) {
			mResults[i] = new RankerResult(evaluatorSize, runSize);
		}
	}

	public void addRun(int algID, int runID, String scoreFile) throws IOException {
		mResults[algID].addRun(runID, scoreFile);
	}

	private void process() {
		for (int i = 0; i < mResults.length; i++) {
			mResults[i].computeSummary();
		}
		
		Arrays.fill(mMaxIndex, 0);
		for (int e = 0; e < mMaxIndex.length; e++) {
			for (int i = 1; i < mResults.length; i++) {
				if (mResults[i].getMean(e) > mResults[mMaxIndex[e]].getMean(e)) {
					mMaxIndex[e] = i;
				}
			}
		}
		
		TTest t = new TTest();
		for (int e = 0; e < mMaxIndex.length; e++) {
			double[] maxResult = mResults[mMaxIndex[e]].getResult(e);
			
			for (int i = 0; i < mResults.length; i++) {
				if (i == mMaxIndex[e]) continue;
				
				mTTestRejected[i][e] = t.pairedTTest(maxResult, mResults[i].getResult(e), ALPHA);
			}
		}
	}

	public void write(String file, RankEvaluator[] evaluators) throws IOException {
		process();		
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("Ranker");
		for (int e = 0; e < evaluators.length; e++) {
			out.write(",Mean " + evaluators[e].getName());
			out.write(",Std " + evaluators[e].getName());
			out.write(",TTest " + evaluators[e].getName());
		}		
		out.newLine();
		
		for (int i = 0; i < mResults.length; i++) {
			out.write(String.valueOf(i));
			for (int e = 0; e < evaluators.length; e++) {
				out.write("," + mResults[i].getMean(e));
				out.write("," + mResults[i].getStd(e));
				out.write("," + (mTTestRejected[i][e] ? "" : "*"));
			}
			out.newLine();
		}
		
		out.close();
	}

}
