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
import gov.ameslab.cydime.util.MapSet;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A facade for all hierarchical feature database.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class DomainDatabase {
	
	public static final String NA = "na";
	
	private Map<String, String> mIPDomainMap;
	private Map<String, String> mIPASNMap;
	private Map<String, String> mIPASNNameMap;
	private MapSet<String, String> mASNIPSet;
	private DomainTree mDomainTree;
	
	public DomainDatabase() {
		mIPDomainMap = CUtil.makeMap();
		mIPASNMap = CUtil.makeMap();
		mIPASNNameMap = CUtil.makeMap();
		mASNIPSet = new MapSet<String, String>();
	}

	public static DomainDatabase load() throws IOException {
		DomainDatabase db = new DomainDatabase();
		
		BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getIPHostMapPath()));
		String line = null;		
		while ((line = in.readLine()) != null) {
			line = line.toLowerCase();			
			String[] split = StringUtil.trimmedSplit(line, ",");
			if (split.length <= 1) {
				in.close();
				throw new IllegalArgumentException("Error reading " + Config.INSTANCE.getIPHostMapPath() + ": " + line);
			}
			
			String ip = split[0];
			if (!NA.equals(split[1])) {
				db.mIPDomainMap.put(ip, split[1]);
			}
		}
		in.close();
				
		in = new BufferedReader(new FileReader(Config.INSTANCE.getIPASNMapPath()));
		line = null;		
		while ((line = in.readLine()) != null) {
			line = line.toLowerCase();			
			String[] split = StringUtil.trimmedSplit(line, ",");
			if (split.length <= 1) {
				in.close();
				throw new IllegalArgumentException("Error reading " + Config.INSTANCE.getIPASNMapPath() + ": " + line);
			}
			
			String ip = split[0];
			if (!NA.equals(split[1])) {
				db.mIPASNMap.put(ip, split[1]);
				if (split.length >= 3) {
					db.mIPASNNameMap.put(ip, split[2]);
				}
				
				db.mASNIPSet.add(split[1], ip);
			}
		}
		in.close();
		
		return db;
	}

	public void loadTree() throws IOException {
		mDomainTree = new DomainTree();
		BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getString(Config.LEXICAL_MISSION_SIM_FILE)));
		String line = null;		
		while ((line = in.readLine()) != null) {
			String[] split = line.split(",");
			String host = split[0];
			double sim = Double.parseDouble(split[4]);
			mDomainTree.addScore(host, sim);
		}
		in.close();
		
		mDomainTree.calcStats();
	}
	
	public void clearTree() {
		mDomainTree = null;
	}
	
	public String getDomain(String ip) {
		return mIPDomainMap.get(ip);
	}
	
	public String getASN(String ip) {
		return mIPASNMap.get(ip);
	}
	
	public String getASNName(String ip) {
		return mIPASNNameMap.get(ip);
	}
	
	public String getASNNameOrNumber(String ip) {
		if (mIPASNNameMap.containsKey(ip)) {
			return mIPASNNameMap.get(ip);
		} else {
			return mIPASNMap.get(ip);
		}		
	}
	
	public double getScore(String ip) {
		String domain = getDomain(ip);
		if (domain == null) return Double.NaN;
		
		return mDomainTree.getScore(domain);
	}
	
	public double getAverageScoreForASN(String asn) {
		double sum = 0.0;
		int count = 0;
		for (String ip : mASNIPSet.get(asn)) {
			double score = getScore(ip);
			if (!Double.isNaN(score)) {
				sum += score;
				count++;
			}
		}
		
		if (count == 0) return Double.NaN;
		return sum / count;
	}

	public List<String> getDomainsForASN(String asn) {
		Set<String> domains = CUtil.makeSet();
		for (String ip : mASNIPSet.get(asn)) {
			String domain = getDomain(ip);
			if (domain != null) {
				domains.add(domain);
			}
		}
		return CUtil.makeList(domains);
	}
	
	public Set<String> getAllIPs() {
		return mIPDomainMap.keySet();
	}
	
	
	public static void main(String[] args) throws IOException {
		Config.INSTANCE.setParam(args[0]);
		DomainDatabase db = DomainDatabase.load();
		db.loadTree();
		db.mDomainTree.print(System.out);
	}

}
