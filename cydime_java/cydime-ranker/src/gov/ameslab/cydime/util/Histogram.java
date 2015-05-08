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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class Histogram<T> {

	private static final DecimalFormat FORMAT = new DecimalFormat("0.00"); 
	
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
		Double count = get(a);
		mHistogram.put(a, count + inc);
	}

	public void remove(T a) {
		mHistogram.remove(a);
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

	public void normalize() {
		double sum = sum();
		if (sum <= 0.0) return;
		
		divide(sum);
	}

	public int size() {
		return mHistogram.size();
	}

	public void add(Histogram<T> o) {
		for (Entry<T, Double> entry : o.entrySet()) {
			increment(entry.getKey(), entry.getValue());
		}
	}

	public void divide(double a) {
		for (T key : mHistogram.keySet()) {
			double v = mHistogram.get(key);
			mHistogram.put(key, v / a);
		}
	}

	public Set<T> keySet() {
		return mHistogram.keySet();
	}
	
	public List<T> getSortedKeysByValue() {
		return CUtil.getSortedKeysByValue(mHistogram);
	}
	
	public T getMaxKeyByValue() {
		T maxKey = null;
		double max = 0.0;
		for (Entry<T, Double> entry : mHistogram.entrySet()) {
			double count = entry.getValue();
			if (count > max) {
				max = count;
				maxKey = entry.getKey();
			}
		}

		return maxKey;
	}

	public void clear() {
		mHistogram.clear();
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
