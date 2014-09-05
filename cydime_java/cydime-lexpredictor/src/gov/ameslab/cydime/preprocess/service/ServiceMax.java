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

package gov.ameslab.cydime.preprocess.service;

import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.Histogram;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceMax extends FeatureSet {
	
	private static final Logger Log = Logger.getLogger(ServiceMax.class.getName());
	
	private static final String FLATTEN = ".Bytes";

	private Map<String, String> mIPServices;
	
	public ServiceMax(List<String> ipList, String inPath, String outPath) {
		super(ipList, inPath, outPath);
	}

	public InstanceDatabase run() throws IOException {
		flatten();
		loadFlatten();
		prepareInstances();
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		return new InstanceDatabase(mCurrentOutPath, mAllIPs);
	}
	
	private void flatten() throws IOException {
		Log.log(Level.INFO, "Processing services (Phase 1)...");

		Map<String, Histogram<String>> ipServices = CUtil.makeMap();
		for (String inPath : mFeaturePaths) {
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				String ip = split[0];
	
				Histogram<String> services = ipServices.get(ip);
				if (services == null) {
					services = new Histogram<String>();
					ipServices.put(ip, services);
				}
				
				//1.0.173.79,udp,udp/domain,2,2,435
				String src = split[1];
				String dest = split[2];
				long value = Long.parseLong(split[5]);
				for (String serv : ServiceParser.parse(src, dest)) {
					services.increment(serv, value);
				}
			}
			in.close();
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(mCurrentOutPath + FLATTEN));
		for (Entry<String, Histogram<String>> entry : ipServices.entrySet()) {
			out.write(entry.getKey());
			out.write(",");
			out.write(entry.getValue().getMaxKeyByValue());
			out.newLine();
		}
		out.close();
	}

	private void loadFlatten() throws IOException {
		Log.log(Level.INFO, "Processing services (Phase 2)...");

		mIPServices = FileUtil.readCSV(mCurrentOutPath + FLATTEN, false);
	}
	
	private void prepareInstances() throws IOException {
		StringBuilder serviceSchema = new StringBuilder();
		serviceSchema.append("service {").append(ServiceParser.SERVICES[0]);
		for (int i = 1; i < ServiceParser.SERVICES.length; i++) {
			serviceSchema.append(",").append(ServiceParser.SERVICES[i]);
		}
		serviceSchema.append("}");
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "services", null,
				serviceSchema.toString()
				); 
		
		for (String ip : mAllIPs) {
			String serv = mIPServices.get(ip);
			out.writeValues(serv, "?");
		}
		
		out.close();
	}

}