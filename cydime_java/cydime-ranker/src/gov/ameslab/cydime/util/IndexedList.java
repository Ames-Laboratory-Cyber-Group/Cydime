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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class IndexedList<T> {

	private List<T> mList;
	private Map<T,Integer> mIndex;
	
	public IndexedList(T ... list) {
		this(Arrays.asList(list));
	}
	
	public IndexedList(List<T> list) {
		mList = list;
		makeIndexMap();
	}

	private void makeIndexMap() {
		mIndex = CUtil.makeMap();
		for (int i = 0; i < mList.size(); i++) {
			T v = mList.get(i);
			mIndex.put(v, i);
		}
	}
	
	public IndexedList(Set<T> set) {
		this(CUtil.makeList(set));
	}

	public T get(int i) {
		return mList.get(i);
	}
	
	public int size() {
		return mList.size();
	}
	
	public boolean contains(T element) {
		return mIndex.containsKey(element);
	}
	
	public int getIndex(T element) {
		if (mIndex.containsKey(element)) {
			return mIndex.get(element);
		} else {
			return -1;
		}
	}

	public int remove(T element) {
		int i = mIndex.remove(element);
		mList.remove(i);
		makeIndexMap();
		return i;
	}

	public void remove(int i) {
		mList.remove(i);
		makeIndexMap();
	}

	public List<T> getList() {
		return mList;
	}
	
	@Override
	public String toString() {
		return mList.toString();
	}

}
