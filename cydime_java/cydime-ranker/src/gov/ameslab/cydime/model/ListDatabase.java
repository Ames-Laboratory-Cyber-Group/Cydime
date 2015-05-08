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

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.NetUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;

public class ListDatabase {

	private List<SubnetUtils> mSubsets;
	private Set<String> mIPs;
	
	public ListDatabase() {
		mSubsets = CUtil.makeList();
		mIPs = CUtil.makeSet();
	}
	
	public static ListDatabase read(String file) throws IOException {
		ListDatabase db = new ListDatabase();
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;		
		while ((line = in.readLine()) != null) {
			line = line.toLowerCase().trim();
			try {
				SubnetUtils subnet = new SubnetUtils(line);
				db.addSubset(subnet);
			} catch (IllegalArgumentException ex) {
				if (NetUtil.isIPv4(line)) {
					db.addIP(line);
				} else {
					System.err.println("Error reading IP: " + line);
				}
			}
		}
		
		in.close();
		
//		System.out.println(db.mSubsets);
//		System.out.println(db.mIPs);
		
		return db;
	}

	public List<SubnetUtils> getSubnets() {
		return mSubsets;
	}

	public Set<String> getIPs() {
		return mIPs;
	}

	public void addSubset(SubnetUtils subnet) {
		mSubsets.add(subnet);
	}

	public void addIP(String ip) {
		mIPs.add(ip);
	}

	public List<List<String>> getList(List<String> ips) {
		Set<String> ipSet = CUtil.makeSet(ips);
		List<List<String>> list = CUtil.makeList();
		
		for (String ip : mIPs) {
			if (ipSet.contains(ip)) {
				list.add(Collections.singletonList(ip));
			}
		}
		
		for (SubnetUtils subnet : mSubsets) {
			Set<String> match = NetUtil.matchSubnet(ipSet, subnet);
			if (!match.isEmpty()) {
				list.add(CUtil.makeList(match));
			}
		}
		
		return list;
	}

	public Set<String> getSet(Set<String> ips) {
		Set<String> ipSet = CUtil.makeSet();
		
		for (String ip : mIPs) {
			if (ips.contains(ip)) {
				ipSet.add(ip);
			}
		}
		
		for (SubnetUtils subnet : mSubsets) {
			Set<String> match = NetUtil.matchSubnet(ips, subnet);
			if (!match.isEmpty()) {
				ipSet.addAll(match);
			}
		}
		
		return ipSet;
	}

}
