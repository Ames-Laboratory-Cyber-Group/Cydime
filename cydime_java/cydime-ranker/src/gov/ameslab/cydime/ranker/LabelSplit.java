package gov.ameslab.cydime.ranker;

import gov.ameslab.cydime.util.CUtil;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LabelSplit {

	private static final Logger Log = Logger.getLogger(LabelSplit.class.getName());
	
	public static final String LABEL_POSITIVE = "1";
	public static final String LABEL_NEGATIVE = "0";
	
	private List<String> mAll;
	private List<String> mTrainWhite;
	private List<String> mTrainBlack;
	private List<String> mTrainUnknown;
	private List<String> mTestWhite;
	private List<String> mTestBlack;
	private List<String> mTestUnknown;
	
	public LabelSplit(List<String> allIPs, List<String> white, List<String> black, double trainPercent, double labelPercent, Random random) {
		mAll = CUtil.makeList(allIPs);
		
		List<String> allUnknown = CUtil.makeList(allIPs);
		allUnknown.removeAll(white);
		allUnknown.removeAll(black);
		
		List<String> allWhite = CUtil.makeList(white);
		List<String> allBlack = CUtil.makeList(black);
		
		Collections.sort(allUnknown);
		Collections.sort(allWhite);
		Collections.sort(allBlack);
		Collections.shuffle(allUnknown, random);
		Collections.shuffle(allWhite, random);
		Collections.shuffle(allBlack, random);
		
		int cutoff = (int) (allUnknown.size() * trainPercent / 100.0);
		mTrainUnknown = allUnknown.subList(0, cutoff);
		mTestUnknown = allUnknown.subList(cutoff, allUnknown.size());

		cutoff = (int) (allWhite.size() * trainPercent / 100.0);
		mTrainWhite = allWhite.subList(0, cutoff);
		mTestWhite = allWhite.subList(cutoff, allWhite.size());
		
		cutoff = (int) (allBlack.size() * trainPercent / 100.0);
		mTrainBlack = allBlack.subList(0, cutoff);
		mTestBlack = allBlack.subList(cutoff, allBlack.size());
		
		cutoff = (int) (mTrainWhite.size() * labelPercent / 100.0);
		mTrainWhite = allWhite.subList(0, cutoff);
		cutoff = (int) (mTrainBlack.size() * labelPercent / 100.0);
		mTrainBlack = allBlack.subList(0, cutoff);
		
		Log.log(Level.INFO, "TrainWhite: " + mTrainWhite.size() + " TrainBlack: " + mTrainBlack.size() + " TrainUnknown: " + mTrainUnknown.size() + " TestWhite: " + mTestWhite.size() + " TestBlack: " + mTestBlack.size() + " TestUnknown: " + mTestUnknown.size());
	}

	public List<String> getAll() {
		return mAll;
	}
	
	public List<String> getTrainWhite() {
		return mTrainWhite;
	}

	public List<String> getTrainBlack() {
		return mTrainBlack;
	}

	public List<String> getTrainUnknown() {
		return mTrainUnknown;
	}
	
	public List<String> getTrainNonWhite() {
		List<String> nonWhite = CUtil.makeList();
		nonWhite.addAll(mTrainBlack);
		nonWhite.addAll(mTrainUnknown);
		return nonWhite;
	}

	public List<String> getTestWhite() {
		return mTestWhite;
	}

	public List<String> getTestBlack() {
		return mTestBlack;
	}
	
	public List<String> getTestUnknown() {
		return mTestUnknown;
	}
	
	public List<String> getTestAll() {
		List<String> all = CUtil.makeList();
		all.addAll(mTestWhite);
		all.addAll(mTestBlack);
		all.addAll(mTestUnknown);
		return all;
	}

	public List<String> getTestKnown() {
		List<String> known = CUtil.makeList();
		known.addAll(mTestWhite);
		known.addAll(mTestBlack);
		return known;
	}
	
}
