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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NetUtil {

	private static final Pattern TWO_NUMBER = Pattern.compile("\\D\\d+\\D\\d+");;

	public static boolean maybeOrgDomain(String domain) {
		if (domain.startsWith("ftp.")) return false;
		
		String[] split = domain.split("\\.");
		if (split.length > 2) {
			return true;
		} else if (split.length == 2) {
			if (domain.length() > 6) return true;
		}
		
		return false;
	}
	
	public static String getOrgDomain(String domain) {
		String[] split = domain.split("\\.");
		if (split.length <= 2) {
			return domain;
		} else {
			String last2 = split[split.length - 2] + "." + split[split.length - 1];
			if (last2.length() > 6) {
				return last2;
			} else {
				return split[split.length - 3] + "." + last2;
			}
		}
	}

	public static boolean hostContainsNumberSegments(String line) {
		Matcher m = TWO_NUMBER.matcher(line);
		return m.find();
	}
	
	public static boolean hostContainsIP(String host, String ip) {
		List<String> checkList = CUtil.makeList();
		String[] split = ip.split("\\.");
		StringBuffer buf = null;
		
		buf = new StringBuffer();
		for (int i = 0; i < split.length; i++) buf.append(split[i]);
		checkList.add(buf.toString());
		
		buf = new StringBuffer();
		for (int i = 0; i < split.length; i++) buf.append(prepend(split[i], "0", 3));
		checkList.add(buf.toString());
		
		buf = new StringBuffer();
		for (int i = split.length - 1; i >= 0; i--) buf.append(split[i]);
		checkList.add(buf.toString());
		
		buf = new StringBuffer();
		for (int i = split.length - 1; i >= 0; i--) buf.append(prepend(split[i], "0", 3));
		checkList.add(buf.toString());
		
		buf = new StringBuffer();
		char[] hostChar = host.toCharArray();
		for (int i = 0; i < hostChar.length; i++) {
			if (Character.isDigit(hostChar[i])) {
				buf.append(hostChar[i]);
			}
		}
		String hostDigit = buf.toString();
		
		for (String c : checkList) {
			if (hostDigit.contains(c)) return true;
		}
		return false;
	}
	
	private static String prepend(String str, String filler, int size) {
		StringBuffer buf = new StringBuffer();
		for (int i = size - str.length(); i > 0; i--) {
			buf.append(filler);
		}
		buf.append(str);
		
		return buf.toString();
	}

	public static void main(String[] args) {
		//true
		System.out.println(hostContainsNumberSegments("77-120-38-188-abc.d-e.com"));
		//true
		System.out.println(hostContainsNumberSegments("a-38-188-abc.d-e.com"));
		//true
		System.out.println(hostContainsNumberSegments("a.38.188abc.d-e.com"));
		//false
		System.out.println(hostContainsNumberSegments("a.38.a.188abc.d-e.com"));
		//false
		System.out.println(hostContainsNumberSegments("kd121109099128.ppp-bb.cxs.ne.gfg"));
		//true
		System.out.println(hostContainsIP("kd121109099128.ppp-bb.cxs.ne.gfg", "121.109.99.128"));
		//true
		System.out.println(hostContainsIP("kd12110999128.ppp-bb.cxs.ne.gfg", "121.109.99.128"));
		//true
		System.out.println(hostContainsIP("77-120-38-188-abc.d-e.com", "77.120.38.188"));
		//true
		System.out.println(hostContainsIP("156.147.55.119.adsl-pool.fff.gg.cn", "119.55.147.156"));
		//true
		System.out.println(hostContainsIP("mobile-166-137-092-236.mmm.nnn", "166.137.92.236"));
		//true
		System.out.println(hostContainsIP("mobile-001-137-092-132.mmm.nnn", "132.92.137.1"));
		//false
		System.out.println(hostContainsIP("dns.defghijk.net", "204.106.240.52"));	
	}

}
