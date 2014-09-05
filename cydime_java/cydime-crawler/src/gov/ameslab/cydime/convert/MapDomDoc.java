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

package gov.ameslab.cydime.convert;

import gov.ameslab.cydime.label.DomainDatabase;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class MapDomDoc {
	
	public static void main(String[] args) throws IOException {
		Set<String> docs = readDocs();
		Set<String> hosts = readHosts();
		
		BufferedWriter out = new BufferedWriter(new FileWriter(Config.INSTANCE.getPath(Config.NEW_DOM_DOC_FILE)));
		for (String host : hosts) {
			String doc = host;
			while (true) {
				if (docs.contains(doc)) {
					out.write(host);
					out.write(",");
					out.write(doc);
					out.newLine();
					break;
				}
				
				int dot = doc.indexOf(".");
				if (dot < 0) {
					break;
				} else {
					doc = doc.substring(dot + 1);
				}
			}
		}
		out.close();
	}

	private static Set<String> readDocs() {
		Set<String> docs = CUtil.makeSet();
		File dir = new File(Config.INSTANCE.getPath(Config.WEB_MERGED_PATH));
		for (File doc : dir.listFiles()) {
			docs.add(doc.getName());
		}
		return docs;
	}

	private static Set<String> readHosts() throws IOException {
		Set<String> hosts = CUtil.makeSet();
		String IP_DOM_FILE = Config.INSTANCE.getPath(Config.NEW_IP_DOM_FILE);
		BufferedReader in = new BufferedReader(new FileReader(IP_DOM_FILE));
		String line = null;		
		while ((line = in.readLine()) != null) {
			String[] values = line.toLowerCase().split(",");
			if (values.length <= 1) {
				System.err.println("Error reading " + IP_DOM_FILE + ": " + line);
				continue;
			} else if (DomainDatabase.IGNORED_DOMAINS.contains(values[1])) continue;
			
			hosts.add(values[1]);
		}
		in.close();
		return hosts;
	}

}
