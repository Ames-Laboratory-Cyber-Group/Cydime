package gov.ameslab.cydime.ranker;

import gov.ameslab.cydime.util.CUtil;

import java.util.List;
import java.util.Random;

public class LabelSample {

	private Random mRandom = new Random(1);
	private List<List<String>> mData;
	
	public LabelSample(List<List<String>> list) {
		mData = list;		
	}

	public List<String> getNextSample() {
		List<String> sample = CUtil.makeList();
		for (List<String> list : mData) {
			int index = mRandom.nextInt(list.size());
			sample.add(list.get(index));
		}		
		return sample;
	}

}
