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

package gov.ameslab.cydime.preprocess;

import gov.ameslab.cydime.util.CUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Matrix<T> {

	private int mISize;
	private int mJSize;
	private T mNullValue;
	private Map<Integer, Map<Integer, T>> mMatrix;
	
	public Matrix(int iSize, int jSize) {
		this(iSize, jSize, null);
	}
	
	public Matrix(int iSize, int jSize, T nullValue) {
		mISize = iSize;
		mJSize = jSize;
		mNullValue = nullValue;
		mMatrix = CUtil.makeMap();
		cTranspose = CUtil.makeMap();
		cAdjArray = CUtil.makeMap();
		cTransposeAdjArray = CUtil.makeMap();
	}

	public int getISize() {
		return mISize;
	}
	
	public int getJSize() {
		return mJSize;
	}
	
	public Map<Integer, Map<Integer, T>> getMatrix() {
		return mMatrix;
	}
	
	public T get(int i, int j) {
		Map<Integer, T> jMap = mMatrix.get(i);
		if (jMap == null) return mNullValue;
		
		T v = jMap.get(j);
		if (v == null) return mNullValue;
		return v;
	}
	
	public void set(int i, int j, T v) {
		set(i, j, v, mMatrix);
	}
	
	private void set(int i, int j, T v, Map<Integer, Map<Integer, T>> ijMap) {
		Map<Integer, T> jMap = ijMap.get(i);
		if (jMap == null) {
			jMap = CUtil.makeMap();
			ijMap.put(i, jMap);
		}
		
		jMap.put(j, v);
	}

	public void remove(int i, int j) {
		remove(i, j, mMatrix);
	}
	
	private void remove(int i, int j, Map<Integer, Map<Integer, T>> ijMap) {
		Map<Integer, T> jMap = ijMap.get(i);
		if (jMap == null) return;
		
		jMap.remove(j);
	}

	public void removeI(int i) {
		mMatrix.remove(i);		
		mISize--;
	}

	public void removeJ(int j) {
		for (Map<Integer, T> jMap : mMatrix.values()) {
			jMap.remove(j);
		}
		mJSize--;
	}

	public Set<Integer> getNeighborsOfI(int i) {
		Map<Integer, T> jMap = mMatrix.get(i);
		if (jMap == null) return Collections.emptySet();
		
		return jMap.keySet();
	}
	
	public Set<Integer> getNeighborsOfJ(int j) {
		Map<Integer, T> iMap = cTranspose.get(j);
		if (iMap == null) return Collections.emptySet();
		
		return iMap.keySet();
	}
	
	////////////////////////////////////////////////
	//Transpose Cache
	////////////////////////////////////////////////
	
	private Map<Integer, Map<Integer, T>> cTranspose;
	
	public Map<Integer, Map<Integer, T>> getTranspose() {
		return cTranspose;
	}
	
	public void updateTranspose() {
		cTranspose.clear();
		for (Entry<Integer, Map<Integer, T>> iEntry : mMatrix.entrySet()) {
			int i = iEntry.getKey();
			for (Entry<Integer, T> jEntry : iEntry.getValue().entrySet()) {
				int j = jEntry.getKey();
				T value = jEntry.getValue();
				set(j, i, value, cTranspose);
			}
		}
	}

	////////////////////////////////////////////////
	//Adjacency Arrays Cache
	////////////////////////////////////////////////
	
	private Map<Integer, int[]> cAdjArray;
	private Map<Integer, int[]> cTransposeAdjArray;
	
	public void updateAdjacencyArrays() {
		cAdjArray.clear();
		for (Entry<Integer, Map<Integer, T>> ijMap : mMatrix.entrySet()) {
			Set<Integer> js = ijMap.getValue().keySet();
			int[] ja = new int[js.size()];
			int index = 0;
			for (int j : js) {
				ja[index++] = j;
			}
			cAdjArray.put(ijMap.getKey(), ja);
		}
		
		cTransposeAdjArray.clear();
		for (Entry<Integer, Map<Integer, T>> jiMap : cTranspose.entrySet()) {
			Set<Integer> is = jiMap.getValue().keySet();
			int[] ia = new int[is.size()];
			int index = 0;
			for (int i : is) {
				ia[index++] = i;
			}
			cTransposeAdjArray.put(jiMap.getKey(), ia);
		}
	}
	
	private int[] EMPTY_ARRAY = new int[0];
	
	public int[] getNeighborListOfI(int i) {
		int[] arr = cAdjArray.get(i);
		if (arr == null) return EMPTY_ARRAY;
		return arr;
	}
	
	public int[] getNeighborListOfJ(int j) {
		int[] arr = cTransposeAdjArray.get(j);
		if (arr == null) return EMPTY_ARRAY;
		return arr;
	}
}
