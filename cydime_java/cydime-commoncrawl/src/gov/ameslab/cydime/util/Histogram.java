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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Histogram<T> {

	private static class Count {
		public int Val = 0;
	}
	
	private Map<T, Count> mHistogram;
	private double mTwoNorm;
	
	public Histogram() {
		mHistogram = CUtil.makeMap();
	}
	
	public double getTwoNorm() {
		return mTwoNorm;
	}

	public int get(T a) {
		Count c = mHistogram.get(a);
		if (c == null) return 0;
		else return c.Val;
	}
	
	public int size() {
		return mHistogram.size();
	}

	public void increment(T a) {
		Count c = mHistogram.get(a);
		if (c == null) {
			c = new Count();
			mHistogram.put(a, c);
		}
		
		c.Val++;
	}
	
	public Set<Entry<T, Count>> entrySet() {
		return mHistogram.entrySet();
	}
	
	public Set<T> keySet() {
		return mHistogram.keySet();
	}
	
	public void clear() {
		mHistogram.clear();
		mTwoNorm = 0.0;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("{ ");
		for (Entry<T, Count> entry : mHistogram.entrySet()) {
			b.append(entry.getKey())
				.append("=")
				.append(entry.getValue().Val)
				.append(" ");
		}
		b.append("}");
		return b.toString();
	}

	public void cacheTwoNorm() {
		mTwoNorm = 0.0;
		for (Count c : mHistogram.values()) {
			mTwoNorm += c.Val * c.Val;
		}
		mTwoNorm = Math.sqrt(mTwoNorm);
	}

	public static <T> double getCosine(Histogram<T> h1, Histogram<T> h2) {
		Histogram<T> hShort = h1;
		Histogram<T> hLong = h2;
		if (h1.size() > h2.size()) {
			hShort = h2;
			hLong = h1;
		}
		
		int sum = 0;
		for (Entry<T, Count> entry : hShort.entrySet()) {
			T key = entry.getKey();
			int vShort = entry.getValue().Val;
			int vLong = hLong.get(key);
			sum += vShort * vLong;
		}
		
		return sum / h1.mTwoNorm / h2.mTwoNorm;
	}
	
}
