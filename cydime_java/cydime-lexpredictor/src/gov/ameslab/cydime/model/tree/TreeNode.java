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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TreeNode {
	
	private String mPartName;
	private Set<String> mIPs;
	private Map<String, TreeNode> mChildren;
	private Set<String> mDescendentIPs;
	
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
	
	public TreeNode(String partName) {
		mPartName = partName;
		mIPs = CUtil.makeSet();
		mChildren = CUtil.makeMap();
		mDomainSplitter = new DomainSplitter();
	}

	public void addDomain(String domain, String ip) {
		mDomainSplitter.split(domain);
		
		TreeNode child = mChildren.get(mDomainSplitter.Next);
		if (child == null) {
			child = new TreeNode(mDomainSplitter.Next);
			mChildren.put(mDomainSplitter.Next, child);
		}
		
		if (mDomainSplitter.Rest == null) {
			child.mIPs.add(ip);
		} else {
			child.addDomain(mDomainSplitter.Rest, ip);
		}
	}

	public void getIPs(String domain, Set<String> result) {
		if (domain == null) {
			result.addAll(mIPs);
			for (TreeNode c : mChildren.values()) {
				c.getIPs(null, result);
			}
		} else {
			mDomainSplitter.split(domain);
			TreeNode c = mChildren.get(mDomainSplitter.Next);
			if (c == null) return;
			
			c.getIPs(mDomainSplitter.Rest, result);
		}
	}

	public void print(PrintStream out, int level) {
		for (int i = 0; i < level; i++) {
			out.print("  ");
		}
		
		out.print(mPartName);
		if (mIPs.isEmpty()) {
			out.println();
		} else {
			out.print(" : ");
			out.println(mIPs);
		}
		
		for (TreeNode c : mChildren.values()) {
			c.print(out, level + 1);
		}
	}

	public void write(BufferedWriter out, String ancestor) throws IOException {
		if (mChildren.isEmpty()) return;
		
		String newAncestor = getFullName(ancestor);
		out.write(newAncestor);
		for (Entry<String, TreeNode> entry : mChildren.entrySet()) {
			TreeNode c = entry.getValue();
			out.write(",");
			out.write(c.getFullName(newAncestor));
		}
		out.newLine();
		
		for (Entry<String, TreeNode> entry : mChildren.entrySet()) {
			TreeNode c = entry.getValue();
			c.write(out, newAncestor);
		}
	}

	private String getFullName(String ancestor) {
		if (mPartName == null) {
			return "*";
		} else {
			return mPartName + "." + ancestor;
		}
	}

	public void countDescendentIPs() {
		mDescendentIPs = CUtil.makeSet(mIPs);
		for (TreeNode c : mChildren.values()) {
			c.countDescendentIPs();
			mDescendentIPs.addAll(c.mDescendentIPs);
		}
	}

	public void reduce(int threshold, Map<String, String> replace, String ancestor) {
		String newAncestor = null;
		if (ancestor == null) {
			newAncestor = mPartName;
		} else {
			newAncestor = mPartName + "." + ancestor;
		}
		
		for (Iterator<Entry<String, TreeNode>> it = mChildren.entrySet().iterator(); it.hasNext(); ) {
			Entry<String, TreeNode> next = it.next();
			TreeNode c = next.getValue();
			
			if (c.mDescendentIPs.size() <= threshold) {
				it.remove();
				mIPs.addAll(c.mDescendentIPs);
				for (String ip : c.mDescendentIPs) {
					replace.put(ip, newAncestor);
				}
			} else {
				c.reduce(threshold, replace, newAncestor);
			}
		}
	}

	public int getDescendentSize() {
		if (mDescendentIPs == null) return -1;
		return mDescendentIPs.size();
	}

	public int getDescendentNodes() {
		int size = 1;
		for (TreeNode c : mChildren.values()) {
			size += c.getDescendentNodes();
		}
		return size;
	}
	
}
