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

package gov.ameslab.cydime.model;

import gov.ameslab.cydime.model.tree.DomainTree;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DomainDatabase {
	
	public static final String NA = "na";
	public static final String DYNAMIC = "dynamic";
	
	public enum DomainType {
		NA,
		STATIC,
		DYNAMIC
	}
	
	private Map<String, String> mDomainMap;
	private Map<String, DomainType> mDomainTypeMap;
	private Map<String, String> mWhoisMap;
	private Map<String, String> mDocMap;
	private DomainTree mDomainTree;
	
	public DomainDatabase() {
		mDomainMap = CUtil.makeMap();
		mDomainTypeMap = CUtil.makeMap();
		mWhoisMap = CUtil.makeMap();
	}

	public static DomainDatabase load() throws IOException {
		DomainDatabase db = new DomainDatabase();
		
		db.mDocMap = FileUtil.readCSV(Config.INSTANCE.getString(Config.DOM_DOC_FILE), true);
		
		BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getString(Config.IP_DOM_FILE)));
		String line = null;		
		while ((line = in.readLine()) != null) {
			line = line.toLowerCase();			
			String[] split = StringUtil.trimmedSplit(line, ",");
			if (split.length <= 1) {
				in.close();
				throw new IllegalArgumentException("Error reading " + Config.INSTANCE.getString(Config.IP_DOM_FILE) + ": " + line);
			}
			
			String ip = split[0];
			if (DYNAMIC.equals(split[1])) {
				db.mDomainTypeMap.put(ip, DomainType.DYNAMIC);
				db.mDomainMap.put(ip, split[2]);
			} else if (!NA.equals(split[1])) {
				db.mDomainTypeMap.put(ip, DomainType.STATIC);
				db.mDomainMap.put(ip, split[1]);
			}
		}
		in.close();
		
		db.buildTree();
		
		in = new BufferedReader(new FileReader(Config.INSTANCE.getString(Config.IP_WHOIS_FILE)));
		line = null;		
		while ((line = in.readLine()) != null) {
			line = line.toLowerCase();			
			String[] split = StringUtil.trimmedSplit(line, ",");
			if (split.length <= 1) {
				in.close();
				throw new IllegalArgumentException("Error reading " + Config.INSTANCE.getString(Config.IP_WHOIS_FILE) + ": " + line);
			}
			
			String ip = split[0];
			if (!NA.equals(split[1])) {
				db.mWhoisMap.put(ip, split[1]);
			}
		}
		in.close();
		
		return db;
	}

	private void buildTree() {
		mDomainTree = new DomainTree();
		for (Entry<String, String> entry : mDomainMap.entrySet()) {
			String ip = entry.getKey();
			String domain = entry.getValue();
			mDomainTree.addDomain(domain, ip);
		}
		
//		mDomainTree.print(System.out);
	}

	public DomainType getDomainType(String ip) {
		DomainType type = mDomainTypeMap.get(ip);
		if (type == null) {
			return DomainType.NA;
		} else {
			return type;
		}
	}
	
	public String getDomain(String ip) {
		return mDomainMap.get(ip);
	}
	
	public String getWhois(String ip) {
		return mWhoisMap.get(ip);
	}
	
	public String getDoc(String ip) {
		if (getDomainType(ip) == DomainType.STATIC) {
			String domain = mDomainMap.get(ip);
			return mDocMap.get(domain);
		} else {
			return null;
		}
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
		return mDomainMap.keySet();
	}
	
}
