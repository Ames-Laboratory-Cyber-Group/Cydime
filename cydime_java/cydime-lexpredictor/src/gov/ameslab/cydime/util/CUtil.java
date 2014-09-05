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

import gov.ameslab.cydime.util.HashMap.ValueFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CUtil {

	public static final Comparator<String> STRING_AS_INT_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			try {
				int i1 = Integer.parseInt(o1);
				int i2 = Integer.parseInt(o2);
				return Integer.compare(i1, i2);
			} catch (NumberFormatException ex) {}
			
			return o1.compareTo(o2);
		}		
	};
	
	public static <T> List<T> makeList() {
		return new ArrayList<T>();
	}

	public static <T> List<T> makeList(Collection<T> as) {
		return new ArrayList<T>(as);
	}

	public static <T> Set<T> makeSet() {
		return new HashSet<T>();
	}

	public static <T> Set<T> makeSet(Collection<T> as) {
		return new HashSet<T>(as);
	}

	public static <K,V> Map<K,V> makeMap() {
		return new HashMap<K,V>();
	}
	
	public static <K,V> gov.ameslab.cydime.util.HashMap<K,V> makeMapWithInitializer(ValueFactory<K, V> f) {
		return new gov.ameslab.cydime.util.HashMap<K,V>(f);
	}

	public static <K,V> Map<K,V> makeMap(Map<K,V> as) {
		return new HashMap<K,V>(as);
	}

	public static <T> Set<T> asSet(T... a) {
		return new HashSet<T>(Arrays.asList(a));
	}

	public static <T> List<String> toStringList(List<T> as) {
		List<String> strings = makeList();
		for (T a : as) {
			strings.add(a.toString());
		}
		return strings;
	}

	public static <K,V> Map<K, V> subMap(Map<K, V> map, Set<K> keySet) {
		Map<K, V> subMap = makeMap();
		for (K key : keySet) {
			subMap.put(key, map.get(key));
		}
		return subMap;
	}

	public static <K> List<K> maxInt(Map<K, Integer> countMap) {
		List<K> result = makeList();
		int max = Integer.MIN_VALUE;
		for (Entry<K, Integer> entry : countMap.entrySet()) {
			int count = entry.getValue();
			if (count > max) {
				max = count;
				result.clear();
				result.add(entry.getKey());
			} else if (count == max) {
				result.add(entry.getKey());
			}
		}

		return result;
	}

	public static <K> List<K> max(Map<K, Double> countMap) {
		List<K> result = makeList();
		double max = 0.0;
		for (Entry<K, Double> entry : countMap.entrySet()) {
			double count = entry.getValue();
			if (count > max) {
				max = count;
				result.clear();
				result.add(entry.getKey());
			} else if (count == max) {
				result.add(entry.getKey());
			}
		}

		return result;
	}

	public static <K> void addInt(Map<K, Integer> to, Map<K, Integer> from) {
		for (Entry<K, Integer> entry : from.entrySet()) {
			K key = entry.getKey();
			Integer toCount = to.get(key);
			if (toCount == null) {
				toCount = 0;
			}
			to.put(key, toCount + entry.getValue());
		}
	}

	public static <K> void add(Map<K, Double> to, Map<K, Double> from) {
		for (Entry<K, Double> entry : from.entrySet()) {
			K key = entry.getKey();
			Double toCount = to.get(key);
			if (toCount == null) {
				toCount = 0.0;
			}
			to.put(key, toCount + entry.getValue());
		}
	}

	private static class Record<K> implements Comparable<Record<K>> {
		K Key;
		Double Value;

		public Record(K key, Double value) {
			Key = key;
			Value = value;
		}
		@Override
		public int compareTo(Record<K> o) {
			return Value.compareTo(o.Value);
		}
	}

	public static <K> List<K> getSortedKeysByValue(final Map<K, Double> as) {
		List<Record<K>> records = makeList();
		for (Entry<K, Double> entry : as.entrySet()) {
			records.add(new Record<K>(entry.getKey(), entry.getValue()));
		}
		Collections.sort(records);

		List<K> sorted = makeList();
		for (Record<K> r : records) {
			sorted.add(r.Key);
		}
		return sorted;
	}

}
