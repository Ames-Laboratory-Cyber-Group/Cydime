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

package gov.ameslab.cydime.util.models;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class ServiceFormatter {

	private Map<String, Integer> mMap;
	
	public ServiceFormatter() {
		try {
			loadMap(Config.INSTANCE.getSilkServicePath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private void loadMap(String file) throws IOException {
		mMap = CUtil.makeMap();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
        while ((line = in.readLine()) != null) {
            String[] split = StringUtil.trimmedSplit(line, " ");
            //6/5666 6/5666 tcp/nrpe
            if (split[2].contains("/")) {
            	int slash = split[0].indexOf("/");
            	String port = split[0].substring(slash + 1);
            	mMap.put(split[2], Integer.parseInt(port));
            }
        }
        in.close();
	}

	public String format(String serv1, String serv2) {
		boolean isServ1HighPort = isHighPort(serv1);
		boolean isServ2HighPort = isHighPort(serv2);
		if (isServ1HighPort && isServ2HighPort) {
			return serv1 + "-" + serv2;
		} else if (!isServ1HighPort && !isServ2HighPort) {
			return serv1 + "-" + serv2;
		} else if (isServ1HighPort) {
			return convertToHigh(serv1) + "-" + serv2;
		} else {
			return serv1 + "-" + convertToHigh(serv2);
		}
	}

	private String convertToHigh(String serv) {
		int slash = serv.indexOf("/");
		if (slash < 0) return serv;
		return serv.substring(0, slash);
	}

	private boolean isHighPort(String serv) {
		Integer port = mMap.get(serv);
		return port == null || port >= 1024;
	}

}
