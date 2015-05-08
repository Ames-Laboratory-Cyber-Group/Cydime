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

package gov.ameslab.cydime.model.tree;

import gov.ameslab.cydime.util.CUtil;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class TreeNode {
	
	private static final int MAX_AVERAGE_COUNT = Integer.MAX_VALUE;//100;
	
	private Map<String, TreeNode> mChildren;
	
	private int mValueSize;
	private double[] cScoreSum;
	private int[] cScoreCount;
	
	private DomainSplitter mDomainSplitter;

	private static class DomainSplitter {
		String Next;
		String Rest;
		
		public void split(String domain) {
			int split = domain.lastIndexOf(".");
			Next = domain;
			Rest = null;
			if (split >= 0) {
				Next = domain.substring(split + 1);
				Rest = domain.substring(0, split);
			}
		}
	}
	
	public TreeNode(int valueSize) {
		//		mChildren = CUtil.makeMap();
		mValueSize = valueSize;
		cScoreSum = new double[valueSize];
		cScoreCount = new int[valueSize];
		mDomainSplitter = new DomainSplitter();
	}

	public void addValue(String domain, int index, double v) {
		mDomainSplitter.split(domain);
		
		if (mChildren == null) {
			mChildren = CUtil.makeMap();
		}
		
		TreeNode child = mChildren.get(mDomainSplitter.Next);
		if (child == null) {
			child = new TreeNode(mValueSize);
			mChildren.put(mDomainSplitter.Next, child);
		}
		
		if (mDomainSplitter.Rest == null) {
			child.cScoreSum[index] += v;
			child.cScoreCount[index]++;
		} else {
			child.addValue(mDomainSplitter.Rest, index, v);
		}
	}

	public double getValue(String domain, int index) {
		mDomainSplitter.split(domain);
		
		if (mChildren == null) {
			return getThresholdedAverage(index);
		}
		
		TreeNode child = mChildren.get(mDomainSplitter.Next);
		if (child == null) {
			return getThresholdedAverage(index);
		}
		
		if (mDomainSplitter.Rest == null) {
			return child.getThresholdedAverage(index);
		} else {
			return child.getValue(mDomainSplitter.Rest, index);
		}
	}

	private double getThresholdedAverage(int index) {
		if (cScoreCount[index] > MAX_AVERAGE_COUNT) {
			return Double.NaN;
		} else {
			return cScoreSum[index] / cScoreCount[index];
		}
	}

	public void calcStats() {
		if (mChildren == null) return;
		
		for (TreeNode c : mChildren.values()) {
			c.calcStats();
			
			for (int i = 0; i < mValueSize; i++) {
				cScoreSum[i] += c.cScoreSum[i];
				cScoreCount[i] += c.cScoreCount[i];
			}
		}
	}	
	
	public void print(PrintStream out, int level) {
		if (mChildren == null) return;
		for (Entry<String, TreeNode> entry : mChildren.entrySet()) {
			TreeNode c = entry.getValue();
			
			for (int i = 0; i < level; i++) {
				out.print("  ");
			}
			
			out.print(entry.getKey());
			out.print(" ");
			out.println(c.cScoreSum + " / " + c.cScoreCount);
			
			c.print(out, level + 1);
		}
	}
	
}
