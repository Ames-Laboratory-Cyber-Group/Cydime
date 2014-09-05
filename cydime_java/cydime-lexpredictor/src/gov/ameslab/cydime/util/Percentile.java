package gov.ameslab.cydime.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Percentile {

	private class Index implements Comparable<Index> {
		public double Value;
		public double Percentile;

		public Index(double v) {
			Value = v;
		}

		@Override
		public int compareTo(Index o) {
			return Double.compare(Value, o.Value);
		}
		
		@Override
		public boolean equals(Object obj) {
			Index o = (Index) obj;
			return compareTo(o) == 0;
		}
		
		@Override
		public int hashCode() {
			return Double.valueOf(Value).hashCode();
		}
	}
	
	private List<Index> mValues;
	private Map<Double, Index> mValueMap;
	
	public Percentile() {
		mValues = CUtil.makeList();
		mValueMap = CUtil.makeMap();
	}
	
	public void add(double v) {
		if (mValueMap.containsKey(v)) return;
		
		Index i = new Index(v);
		mValues.add(i);
		mValueMap.put(i.Value, i);
	}

	public void compute() {
		Collections.sort(mValues);
		for (int i = 0; i < mValues.size(); i++) {
			mValues.get(i).Percentile = (double) i / mValues.size();
		}
	}

	public double getPercentile(double v) {
		return mValueMap.get(v).Percentile;
	}

}
