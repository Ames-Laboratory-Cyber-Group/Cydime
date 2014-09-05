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

package gov.ameslab.cydime.label;

import gov.ameslab.cydime.label.tree.DomainTree;
import gov.ameslab.cydime.util.CUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DomainDatabase {
	
	public static final Set<String> IGNORED_DOMAINS = CUtil.asSet("na", "dynamic");
	
	private Map<String, String> mMap;
	private DomainTree mDomainTree;
	
	public DomainDatabase() {
		mMap = CUtil.makeMap();
	}

	public static DomainDatabase readCSV(String file) throws IOException {
		DomainDatabase db = new DomainDatabase();
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;		
		while ((line = in.readLine()) != null) {
			String[] values = line.toLowerCase().split(",");
			if (values.length <= 1) {
				System.err.println("Error reading " + file + ": " + line);
				continue;
			} else if (IGNORED_DOMAINS.contains(values[1])) continue;
			
			db.mMap.put(values[0].trim(), values[1].trim());
		}
		
		in.close();
		
//		System.out.println(db.mMap);
		
		db.buildTree();
		
		return db;
	}

	private void buildTree() {
		mDomainTree = new DomainTree();
		for (Entry<String, String> entry : mMap.entrySet()) {
			mDomainTree.addDomain(entry.getValue(), entry.getKey());
		}
		
//		mDomainTree.print(System.out);
	}

	public String getDomain(String ip) {
		return mMap.get(ip);
	}

	public Set<String> getIPs(String domain) {
		return mDomainTree.getIPs(domain);
	}
	
	public Set<String> getLeafDomains(String domain) {
		Set<String> leaves = CUtil.makeSet();
		for (String ip : mDomainTree.getIPs(domain)) {
			leaves.add(getDomain(ip));
		}
		return leaves;
	}
	
	public Set<String> getAllIPs() {
		return mMap.keySet();
	}

	public List<String> getAllDomainSuffixes(int beginDepth, int childrenSizeThreshold) {
		return mDomainTree.getAllSuffixes(beginDepth, childrenSizeThreshold);
	}
	
}
