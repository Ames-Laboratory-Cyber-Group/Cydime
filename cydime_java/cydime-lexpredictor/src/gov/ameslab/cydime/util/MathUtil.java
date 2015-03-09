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

import gov.ameslab.cydime.preprocess.community.Matrix;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class MathUtil {
	
	public static Random Random = new Random(0);

	private static final double EPSILON = 1.0E-10;
	
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
	
	public static int nextPower(int v, int power) {
		int result = 1;
		while (result < v) {
			result *= power;
		}
		return result;
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

	public static double sum(double[] a) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum;
	}

	public static double sum(double[][] a) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += sum(a[i]);
		}
		return sum;
	}
	
	public static double[] sumDimension(double[][] matrix, int dim) {
		if (dim == 1) {
			double[] sum = new double[matrix[0].length];
			for (int s = 0; s < sum.length; s++) {
				for (int i = 0; i < matrix.length; i++) {
					sum[s] += matrix[i][s];
				}
			}
			return sum;
		} else if (dim == 2) {
			double[] sum = new double[matrix.length];
			for (int s = 0; s < sum.length; s++) {
				for (int i = 0; i < matrix[0].length; i++) {
					sum[s] += matrix[s][i];
				}
			}
			return sum;
		} else return null;
	}
	
	public static double sum(Matrix<Double> matrix) {
		double sum = 0.0;
		for (Map<Integer, Double> jMap : matrix.getMatrix().values()) {
			for (double v : jMap.values()) {
				sum += v;
			}
		}
		return sum;
	}
	
	public static double[] sumDimension(Matrix<Double> matrix, int dim) {
		if (dim == 1) {
			double[] sum = new double[matrix.getJSize()];
			for (int s = 0; s < matrix.getJSize(); s++) {
				Map<Integer, Double> iMap = matrix.getTranspose().get(s);
				if (iMap == null) continue;
				
				for (double v : iMap.values()) {
					sum[s] += v;
				}
			}
			return sum;
		} else if (dim == 2) {
			double[] sum = new double[matrix.getISize()];
			for (int s = 0; s < matrix.getISize(); s++) {
				Map<Integer, Double> jMap = matrix.getMatrix().get(s);
				if (jMap == null) continue;
				
				for (double v : jMap.values()) {
					sum[s] += v;
				}
			}
			return sum;
		} else return null;
	}
	
	public static int maxIndex(double[] a) {
		double currentMax = a[0];
		int currentMaxIndex = 0;
		for (int i = 1; i < a.length; i++) {
			if (a[i] > currentMax) {
				currentMax = a[i];
				currentMaxIndex = i;
			}
		}
		return currentMaxIndex;
	}

	public static double max(double[] a) {
		double max = a[0];
		for (int i = 1; i < a.length; i++) {
			if (a[i] > max) {
				max = a[i];
			}
		}
		return max;
	}

	private static double max(double[] a, int begin, int end) {
		double max = a[begin];
		for (int i = begin + 1; i < end; i++) {
			if (a[i] > max) {
				max = a[i];
			}
		}
		return max;
	}
	
	public static void divide(double[] a, double sum) {
		for (int i = 0; i < a.length; i++) {
			a[i] /= sum;
		}
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

	public static double mean(double[] a) {
		return mean(a, 0, a.length);
	}
	
	public static double mean(double[] a, int from, int to) {
		double mean = 0.0;
		for (int i = from; i < to; i++) {
			mean += a[i];
		}
		return mean / (to - from);
	}

	public static double variance(double[] a, int from, int to) {
		double mean = mean(a, from, to);
		double result = 0.0;
		for (int i = from; i < to; i++) {
			double d = a[i] - mean;
			result += d * d;
		}
		return result / (to - from);
	}

	public static <T> Map<T, Double> normalize(Map<T, Double> count) {
		Map<T, Double> result = CUtil.makeMap();
		Collection<Double> values = count.values();
		double sum = MathUtil.sum(values);
		for (Entry<T, Double> entry: count.entrySet()) {
			double v = entry.getValue() / sum;
			result.put(entry.getKey(), v);
		}
		return result;
	}
	
	public static void rescale(double[] a) {
		double min = a[0];
		double max = a[0];
		for (int i = 1; i < a.length; i++) {
			if (a[i] < min) {
				min = a[i];
			}
			if (a[i] > max) {
				max = a[i];
			}
		}
		
		double range = max - min;
		for (int i = 0; i < a.length; i++) {
			a[i] = (a[i] - min) / range;
		}
	}

	public static <T> void rescale(Map<T, Double> count) {
		Double min = null;
		Double max = null;
		for (Double v : count.values()) {
			if (min == null || v < min) {
				min = v;
			}
			if (max == null || v > max) {
				max = v;
			}
		}
		
		double range = max - min;
		for (T key : new HashSet<T>(count.keySet())) {
			double v = count.get(key);
			v = (v - min) / range;
			count.put(key, v);
		}
	}

	static double sum(Collection<Double> as) {
		double sum = 0;
		for (double a : as) {
			sum += a;
		}
		return sum;
	}

	public static int sample(double[] a) {
		//a should be normalized
		
		double r = Random.nextDouble();
		if (r <= a[0]) return 0;
		
		double sum = a[0];
		for (int i = 1; i < a.length; i++) {
			double newSum = sum + a[i];
			if (sum < r && r <= newSum) return i;
			sum = newSum;
		}
		
		return -1;
	}

	public static double divergenceKL(double ... a) {
		double result = 0.0;
		double mean = boundBelow(mean(a), 0.0);
		for (int i = 0; i < a.length; i++) {
			result += divergenceKL(a[i], mean);
		}
		return result / a.length;
	}
	
	public static double divergenceKL(double pa, double pb) {
		pa = boundBelow(pa, 0.0);
		double pA = boundBelow(1.0 - pa, 0.0);
		double pB = 1.0 - pb;
		double result = pa * (Math.log(pa) - Math.log(pb)) +
		pA * (Math.log(pA) - Math.log(pB));
		
		if (Double.isNaN(result)) {
			System.out.println(pa + " " + pb);
			System.out.println(Math.log(pa) + " " + Math.log(pb));
			System.out.println(Math.log(pA) + " " + Math.log(pB));
		}
		return result;
	}
	
	public static double boundBelow(double a, double lower) {
		if (a <= lower + EPSILON) return lower + EPSILON;
		return a;
	}

	public static double sumLog(double[] logs) {
		return sumLog(logs, 0, logs.length);
	}

	public static double sumLog(double[] logs, int begin, int end) {
		double max = max(logs, begin, end);
		double norm = 0.0;
		for (int i = begin; i < end; i++) {
			norm += Math.exp(logs[i] - max);
		}
		norm = Math.log(norm) + max;
		return norm;
	}
	
}
