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

package gov.ameslab.cydime.util;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Histogram<T> {

	private static final DecimalFormat FORMAT = new DecimalFormat("0.00"); 
	
	static class Element<T> implements Comparable<Element<T>> {
		
		public T Key;
		public Double Count;
		
		public Element(Entry<T, Double> entry) {
			Key = entry.getKey();
			Count = entry.getValue();
		}

		@Override
		public int compareTo(Element<T> o) {
			return Double.compare(o.Count, Count);
		}

		public T getKey() {
			return Key;
		}

	}
	
	private Map<T,Double> mHistogram;
	
	public Histogram() {
		mHistogram = CUtil.makeMap();
	}

	public double get(T a) {
		Double v = mHistogram.get(a);
		if (v == null) return 0.0;
		else return v;
	}

	public void increment(T a) {
		increment(a, 1.0);
	}
	
	public void increment(T a, double inc) {
		Double count = mHistogram.get(a);
		if (count == null) {
			count = 0.0;
		}
		mHistogram.put(a, count + inc);
	}

	public void min(T a, double v) {
		Double count = mHistogram.get(a);
		if (count == null || v < count) {
			count = v;
			mHistogram.put(a, count);
		}
	}

	public Set<Entry<T, Double>> entrySet() {
		return mHistogram.entrySet();
	}

	public double sum() {
		double sum = 0.0;
		for (double v : mHistogram.values()) {
			sum += v;
		}
		return sum;
	}

	public void add(Histogram<T> o) {
		for (Entry<T, Double> entry : o.entrySet()) {
			increment(entry.getKey(), entry.getValue());
		}
	}
	
	public List<T> getTop(int K) {
		List<Element<T>> list = CUtil.makeList();
		for (Entry<T, Double> entry : mHistogram.entrySet()) {
			list.add(new Element<T>(entry));
		}
		Collections.sort(list);
		
		List<T> top = CUtil.makeList();
		for (int i = 0; i < K; i++) {
			top.add(list.get(i).getKey());
		}
		return top;
	}

	public List<T> getSortedKeysByValue() {
		return CUtil.getSortedKeysByValue(mHistogram);
	}

	public void replaceNanWith(double target) {
		for (T key : mHistogram.keySet()) {
			Double v = mHistogram.get(key);
			if (v.isNaN()) {
				mHistogram.put(key, target);
			}
		}
	}
	
	public void replaceInfiniteWith(double target) {
		for (T key : mHistogram.keySet()) {
			Double v = mHistogram.get(key);
			if (v.isInfinite()) {
				mHistogram.put(key, target);
			}
		}
	}

	public void normalizeTo(double targetMin, double targetMax) {
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (Double v : mHistogram.values()) {
			if (v.isInfinite() || v.isNaN()) continue;
			
			if (v < min) {
				min = v;
			}
			if (v > max) {
				max = v;
			}
		}
		
		double range = max - min;
		double targetRange = targetMax - targetMin;
		for (T key : mHistogram.keySet()) {
			Double v = mHistogram.get(key);
			if (v.isInfinite() || v.isNaN()) continue;
			
			mHistogram.put(key, targetMin + (v - min) * targetRange / range);
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("{ ");
		for (Entry<T, Double> entry : mHistogram.entrySet()) {
			b.append(entry.getKey())
				.append("=")
				.append(FORMAT.format(entry.getValue()))
				.append(" ");
		}
		b.append("}");
		return b.toString();
	}

}
