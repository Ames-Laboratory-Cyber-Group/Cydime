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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MathUtil {

	private static class Element implements Comparable<Element> {
		int I;
		double V;
		
		public Element(int i, double v) {
			I = i;
			V = v;
		}

		@Override
		public int compareTo(Element o) {
			return Double.compare(V, o.V);
		}
	}
	
	public static List<Integer> topIndex(double[] v, int top) {
		List<Element> list = CUtil.makeList();
		for (int i = 0; i < v.length; i++) {
			list.add(new Element(i, v[i]));
		}
		
		Collections.sort(list);
		Collections.reverse(list);
		List<Integer> result = CUtil.makeList();
		for (int i = 0; i < top; i++) {
			result.add(list.get(i).I);
		}
		return result;
	}
	
	public static List<Integer> topIndexByWeight(double[] v, double percent) {
		double sum = sum(v);
		List<Element> list = CUtil.makeList();
		for (int i = 0; i < v.length; i++) {
			list.add(new Element(i, v[i] / sum));
		}
		
		Collections.sort(list);
		Collections.reverse(list);
		List<Integer> result = CUtil.makeList();
		double current = 0.0;
		for (int i = 0; i < v.length; i++) {
			Element e = list.get(i);
			result.add(e.I);
			current += e.V;
			if (current >= percent) break;
		}
		return result;
	}
	
	public static void normalize(double[] a) {
		double sum = sum(a);
		if (sum <= 0.0) {
			Arrays.fill(a, 1.0 / a.length);
			return;
		}

		divide(a, sum);
		sum = sum(a);
		a[a.length - 1] = 1.0 - (sum - a[a.length - 1]); 
	}
	
	public static void divide(double[] a, double v) {
		for (int i = 0; i < a.length; i++) {
			a[i] /= v;
		}
	}
	
	public static double sum(double[] a) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum;
	}

}
