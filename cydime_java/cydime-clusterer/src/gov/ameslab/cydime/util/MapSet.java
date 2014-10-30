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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MapSet<K,T> {

	private Map<K,Set<T>> mMapSet;
	
	public MapSet() {
		mMapSet = new HashMap<K,Set<T>>();
	}
	
	public void put(K key, Set<T> s) {
		mMapSet.put(key, s);
	}

	public void add(K key, T v) {
		Set<T> set = mMapSet.get(key);
		if (set == null) {
			set = new HashSet<T>();
			put(key, set);
		}
		set.add(v);
	}
	
	public Set<T> get(K key) {
		Set<T> set = mMapSet.get(key);
		if (set == null) return Collections.emptySet();
		else return set;
	}

	public boolean contains(K key, T v) {
		return get(key).contains(v);
	}
	
	public Set<K> keySet() {
		return mMapSet.keySet();
	}

	public Set<Entry<K, Set<T>>> entrySet() {
		return mMapSet.entrySet();
	}

	public void removeKey(K key) {
		mMapSet.remove(key);
	}
	
	public void remove(K key, T v) {
		Set<T> set = mMapSet.get(key);
		if (set != null) {
			set.remove(v);
		}
	}
	
	public int size() {
		return mMapSet.size();
	}

	public int sizeOf(K key) {
		return mMapSet.get(key).size();
	}
	
	@Override
	public String toString() {
		return mMapSet.toString();
	}

	public static MapSet<String, String> readCSV(String file) throws IOException {
		MapSet<String, String> mapset = new MapSet<String, String>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		String line = null;
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			for (int i = 1; i < split.length; i++) {
				mapset.add(split[0], split[i]);
			}
		}
		in.close();
		return mapset;
	}

	public <V> MapSet<K, V> compose(MapSet<T, V> map) {
		MapSet<K, V> result = new MapSet<K, V>();
		for (Entry<K, Set<T>> entry : mMapSet.entrySet()) {
			K key = entry.getKey();
			for (T t : entry.getValue()) {
				for (V v : map.get(t)) {
					result.add(key, v);
				}
			}
		}		
		return result;
	}

	public void writeCSV(String file) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		for (Entry<K, Set<T>> entry : mMapSet.entrySet()) {
			out.write(entry.getKey().toString());
			for (T t : entry.getValue()) {
				out.write(",");
				out.write(t.toString());
			}
			out.newLine();
		}
	
		out.close();
	}

}
