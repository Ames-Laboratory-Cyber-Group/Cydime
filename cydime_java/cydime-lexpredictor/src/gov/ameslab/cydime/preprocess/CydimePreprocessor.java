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

package gov.ameslab.cydime.preprocess;

import gov.ameslab.cydime.model.DomainDatabase;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.community.BiGraph;
import gov.ameslab.cydime.preprocess.dailyprofile.DailyProfile;
import gov.ameslab.cydime.preprocess.dailyprofile.DailyProfile.Normalizer;
import gov.ameslab.cydime.preprocess.hierarchy.Hostname;
import gov.ameslab.cydime.preprocess.netflow.Netflow;
import gov.ameslab.cydime.preprocess.service.ServiceMax;
import gov.ameslab.cydime.preprocess.timeseries.TimeAccess;
import gov.ameslab.cydime.preprocess.timeseries.TimeSeries;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cydime Preprocessor.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimePreprocessor {
	
	private static final Logger Log = Logger.getLogger(CydimePreprocessor.class.getName());
	
	private DomainDatabase mDomainDB;
	private List<String> mExtIPs;
	private List<String> mIntIPs;
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			printUsage();
			return;
		}
		
		new CydimePreprocessor(args[0]).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimePreprocessor FEATURE_DIR");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
	}
	
	public CydimePreprocessor(String datePath) throws IOException {
		Config.INSTANCE.setParam(datePath);
		mDomainDB = DomainDatabase.load();
	}
	
	private void run() throws IOException {
		runExt();
		runInt();
	}
	
	private void runExt() throws IOException {
		loadExtIPs();
		
		InstanceDatabase hierarchy = new Hostname(mExtIPs, Config.INSTANCE.getHierarchy(), Config.INSTANCE.getHierarchy(), mDomainDB).run();
		loadLabels(hierarchy);
		hierarchy.saveIPs();
		hierarchy.write();
		hierarchy.writeReport();
		
		InstanceDatabase service = new ServiceMax(mExtIPs, Config.INSTANCE.getService(), Config.INSTANCE.getService()).run();
		InstanceDatabase netflow = new Netflow(mExtIPs, Config.INSTANCE.getNetflow(), Config.INSTANCE.getNetflow()).run();
		InstanceDatabase ts = new TimeSeries(mExtIPs, Config.INSTANCE.getTimeSeries(), Config.INSTANCE.getTimeSeries()).run();
		InstanceDatabase ta = new TimeAccess(mExtIPs, Config.INSTANCE.getTimeSeries(), Config.INSTANCE.getTimeAccess()).run();
		InstanceDatabase dpService = new DailyProfile(mExtIPs, Config.INSTANCE.getDailyProfile(), Config.INSTANCE.getDailyProfile()).run(Normalizer.SERVICE_SUM);
		InstanceDatabase dpTime = new DailyProfile(mExtIPs, Config.INSTANCE.getDailyProfile(), Config.INSTANCE.getDailyProfile()).run(Normalizer.TIME_SUM);
		
		InstanceDatabase base = InstanceDatabase.mergeFeatures(Config.INSTANCE.getBasePath(),
				service,
				netflow,
				ts,
				ta,
				dpService,
				dpTime
				);
		
		loadLabels(base);
		
		base.saveIPs();
		base.write();
		base.writeReport();
		
		FileUtil.copy(base.getIPPath(), Config.INSTANCE.getBaseNormPath() + WekaPreprocess.IP_SUFFIX);
		FileUtil.copy(base.getARFFPath(), Config.INSTANCE.getBaseNormPath() + WekaPreprocess.ALL_SUFFIX);
		FileUtil.copy(base.getReportARFFPath(), Config.INSTANCE.getBaseNormPath() + WekaPreprocess.REPORT_SUFFIX);
		InstanceDatabase baseNorm = InstanceDatabase.load(Config.INSTANCE.getBaseNormPath());
		baseNorm.normalize();
		baseNorm.writeReport();
		
		//for Explorer
		new BiGraph(mExtIPs, Config.INSTANCE.getPairService(), Config.INSTANCE.getPairService()).run();
	}

	private void loadExtIPs() throws IOException {
		Set<String> netIPs = readIPs(Config.INSTANCE.getNetflow(), 0);
		Set<String> servIPs = readIPs(Config.INSTANCE.getService(), 0);
		Set<String> timeIPs = readIPs(Config.INSTANCE.getTimeSeries(), 0);
		Set<String> pairServIPs = readIPs(Config.INSTANCE.getPairService(), 1);
		Set<String> allIPs = netIPs;
		allIPs.retainAll(servIPs);
		allIPs.retainAll(timeIPs);
		allIPs.retainAll(pairServIPs);
		
		mExtIPs = CUtil.makeList(allIPs);
		Collections.sort(mExtIPs);

		Log.log(Level.INFO, "External IP set = {0}", mExtIPs.size() );
	}
	
	//For experiment
//	private void loadExtIPs() throws IOException {
//		Set<String> netIPs = readNetflowIPs(Config.INSTANCE.getNetflow(), 0);
//		Set<String> servIPs = readIPs(Config.INSTANCE.getService(), 0);
//		Set<String> timeIPs = readIPs(Config.INSTANCE.getTimeSeries(), 0);
//		Set<String> pairServIPs = readIPs(Config.INSTANCE.getPairService(), 1);
//		Set<String> pairServTimeIPs = readNetflowIPs(Config.INSTANCE.getDailyProfile(), 1);
//		Set<String> allIPs = netIPs;
//		allIPs.retainAll(servIPs);
//		allIPs.retainAll(timeIPs);
//		allIPs.retainAll(pairServIPs);
//		allIPs.retainAll(pairServTimeIPs);
//		
//		Set<String> domains = CUtil.makeSet();
//		mExtIPs = CUtil.makeList();
//		List<String> ipProfile = CUtil.makeList();
//		for (String ip : allIPs) {
//			String doc = mDomainDB.getDoc(ip);
//			if (doc == null) continue;
//			
//			String domain = mDomainDB.getDomain(ip);
//			String whois = mDomainDB.getWhois(ip);
//			if (domain == null || domains.contains(domain)) continue;
//			domains.add(domain);
//			
//			ipProfile.add(ip + "," + domain + "," + whois);
//			mExtIPs.add(ip);
//		}
//		Collections.sort(mExtIPs);
//
//		FileUtil.writeFile(Config.INSTANCE.getCurrentPreprocessPath() + "ext_web.ip.csv", ipProfile);
//		
//		Log.log(Level.INFO, "External IP set = {0}", mExtIPs.size() );
//	}
		
	private void runInt() throws IOException {
		loadIntIPs();
		
		InstanceDatabase netflow = new Netflow(mIntIPs, Config.INSTANCE.getIntNetflow(), Config.INSTANCE.getIntNetflow()).run();
		InstanceDatabase ts = new TimeSeries(mIntIPs, Config.INSTANCE.getIntTimeSeries(), Config.INSTANCE.getIntTimeSeries()).run();
		InstanceDatabase ta = new TimeAccess(mIntIPs, Config.INSTANCE.getIntTimeSeries(), Config.INSTANCE.getIntTimeAccess()).run();
		
		InstanceDatabase baseInt = InstanceDatabase.mergeFeatures(Config.INSTANCE.getIntBasePath(),
				netflow,
				ts,
				ta);

		baseInt.saveIPs();
		baseInt.writeReport();
		
		FileUtil.copy(baseInt.getIPPath(), Config.INSTANCE.getIntBaseNormPath() + WekaPreprocess.IP_SUFFIX);
		FileUtil.copy(baseInt.getARFFPath(), Config.INSTANCE.getIntBaseNormPath() + WekaPreprocess.ALL_SUFFIX);
		FileUtil.copy(baseInt.getReportARFFPath(), Config.INSTANCE.getIntBaseNormPath() + WekaPreprocess.REPORT_SUFFIX);
		InstanceDatabase baseIntNorm = InstanceDatabase.load(Config.INSTANCE.getIntBaseNormPath());
		baseIntNorm.normalize();
		baseIntNorm.writeReport();
	}

	private void loadIntIPs() throws IOException {
		Set<String> netIPs = readIPs(Config.INSTANCE.getIntNetflow(), 0);
		Set<String> timeIPs = readIPs(Config.INSTANCE.getIntTimeSeries(), 0);
		Set<String> allIPs = netIPs;
		allIPs.retainAll(timeIPs);
		
		mIntIPs = CUtil.makeList(allIPs);
		Collections.sort(mIntIPs);
		
		Log.log(Level.INFO, "Internal IP set = {0}", mIntIPs.size() );
	}
	

	static class IPData {
		public String IP;
		public double Data;
		public IPData(String ip, double data) {
			IP = ip;
			Data = data;
		}		
	}
	
	private static Set<String> readNetflowIPs(String file, int ipIndex) throws IOException {
		Set<String> ips = CUtil.makeSet();
		BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getCurrentFeaturePath() + file));
		String line = in.readLine();		
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			ips.add(split[ipIndex]);
		}

		in.close();

		Log.log(Level.INFO, "Read {0} IPs from {1}", new Object[] {ips.size(), file} );
		return ips;
	}

	private static Set<String> readIPs(String file, int ipIndex) throws IOException {
		Set<String> ips = CUtil.makeSet();
		for (String featurePath : Config.INSTANCE.getFeaturePaths()) {
			BufferedReader in = new BufferedReader(new FileReader(featurePath + file));
			String line = in.readLine();		
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				ips.add(split[ipIndex]);
			}
			
			in.close();
		}
		
		Log.log(Level.INFO, "Read {0} IPs from {1}", new Object[] {ips.size(), file} );
		return ips;
	}

	private void loadLabels(InstanceDatabase base) throws IOException {
		Map<String, String> docSims = FileUtil.readCSV(Config.INSTANCE.getString(Config.LEXICAL_MISSION_SIM_FILE), 1, true);
		
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		
		for (String ip : base.getIPs()) {
			String doc = mDomainDB.getDoc(ip);
			if (doc != null) {
				String simStr = docSims.get(doc);
				if (simStr == null) {
					System.err.println("Error: Mission similarity not found for " + doc);
					continue;
				}
				
				double sim = Double.parseDouble(simStr);
				if (sim < min) {
					min = sim;
				}
				if (sim > max) {
					max = sim;
				}
			}
		}
		
		for (String ip : base.getIPs()) {
			String doc = mDomainDB.getDoc(ip);
			if (doc != null) {
				String simStr = docSims.get(doc);
				if (simStr == null) continue;
				
				double sim = Double.parseDouble(simStr);
				base.setLabel(ip, (sim - min) / (max - min));
			}
		}
	}

}
