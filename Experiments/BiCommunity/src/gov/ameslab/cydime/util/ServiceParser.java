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

import java.util.Set;

/**
 * Maps the protocol/port of a traffic with a set of common services.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class ServiceParser {

	public static final String SERVICE_ICMP = "icmp";
	public static final String SERVICE_SSH = "ssh";
	public static final String SERVICE_SMTP = "smtp";
	public static final String SERVICE_DOMAIN = "domain";
	public static final String SERVICE_RTSP = "rtsp";
	public static final String SERVICE_HTTP = "http";
	public static final String SERVICE_MAIL = "mail";
	public static final String SERVICE_VPN = "vpn";
	public static final String SERVICE_OTHER = "OTHER";
	
	public static final String[] SERVICES = new String[] {
		SERVICE_ICMP,
		SERVICE_SSH,
		SERVICE_SMTP,
		SERVICE_DOMAIN,
		SERVICE_RTSP,
		SERVICE_HTTP,
		SERVICE_MAIL,
		SERVICE_VPN,
		SERVICE_OTHER
	} ;
	
	public static Set<String> parse(String src, String dest) {
		Set<String> result = CUtil.makeSet();
		if (src.equals(dest)) {
			result.add(getServiceCategory(src));
		} else {
			int srcSlash = src.indexOf("/");
			int destSlash = dest.indexOf("/");
			
			if (srcSlash >= 0 && destSlash < 0) {
				result.add(getServiceCategory(src));
			} else if (srcSlash < 0 && destSlash >= 0) {
				result.add(getServiceCategory(dest));
			} else {
				result.add(getServiceCategory(src));
				result.add(getServiceCategory(dest));
			}
		}
		return result;
	}

	private static String getServiceCategory(String v) {
		if (v.startsWith("ICMP")) {
			return SERVICE_ICMP;
		} else if (v.equalsIgnoreCase("tcp/ssh")) {
			return SERVICE_SSH;
		} else if (v.equalsIgnoreCase("tcp/smtp")) {
			return SERVICE_SMTP;
		} else if (v.endsWith("/domain")) {
			return SERVICE_DOMAIN;
		} else if (v.endsWith("/rtsp")) {
			return SERVICE_RTSP;
		} else if (v.contains("/http")) {
			return SERVICE_HTTP;
		} else if (v.contains("/imap") || v.contains("/pop")) {
			return SERVICE_MAIL;
		} else if (v.contains("/l2f")) {
			return SERVICE_VPN;
		} else {
			return SERVICE_OTHER;
		}
	}
	
}
