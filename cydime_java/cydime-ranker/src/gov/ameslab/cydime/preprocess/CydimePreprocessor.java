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
import gov.ameslab.cydime.preprocess.lexical.Lexical;
import gov.ameslab.cydime.preprocess.netflow.Netflow;
import gov.ameslab.cydime.preprocess.service.ServiceMax;
import gov.ameslab.cydime.preprocess.timeseries.TimeAccess;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
	private List<String> mIDs;
	
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
	
	private void run() {
		try {
			runExtIP();
		} catch (IOException e) {
			Log.log(Level.SEVERE, e.toString());
			e.printStackTrace();
		}
		
//		try {
//			runInt();
//		} catch (IOException e) {
//			Log.log(Level.SEVERE, e.toString());
//			e.printStackTrace();
//		}
	}
	
	private void runExtIP() throws IOException {
		Config.INSTANCE.setFeatureDir(Config.IP_DIR);
		loadExtIDs();
		
		InstanceDatabase service = new ServiceMax(mIDs, Config.INSTANCE.getService(), Config.INSTANCE.getService()).run();
		InstanceDatabase netflow = new Netflow(mIDs, Config.INSTANCE.getNetflow(), Config.INSTANCE.getNetflow()).run();
		InstanceDatabase ta = new TimeAccess(mIDs, Config.INSTANCE.getTimeSeries(), Config.INSTANCE.getTimeAccess()).run();
		
		mDomainDB.loadTree();
		InstanceDatabase lexical = new Lexical(mIDs, Config.INSTANCE.getLexical(), Config.INSTANCE.getLexical(), mDomainDB).run();
		mDomainDB.clearTree();
				
		InstanceDatabase base = InstanceDatabase.mergeFeatures(Config.INSTANCE.getBasePath(),
				service,
				netflow,
				ta,
				lexical
				);		
		service = null;
		netflow = null;
		ta = null;
		lexical = null;
		
		base.saveIPs();
		base.write();
		base.writeReport();
		
		new FeatureCombiner().run();

		//for Explorer
//		new BiGraph(mIDs, Config.INSTANCE.getPairService(), Config.INSTANCE.getPairService()).run();
	}

	private void loadExtIDs() throws IOException {
		Set<String> netIPs = readIDs(Config.INSTANCE.getNetflow(), 0, Config.INSTANCE.getCurrentFeaturePath());
		Set<String> servIPs = readIDs(Config.INSTANCE.getService(), 0, Config.INSTANCE.getCurrentFeaturePath());
		Set<String> timeIPs = readIDs(Config.INSTANCE.getTimeSeries(), 0, Config.INSTANCE.getCurrentFeaturePath());
//		Set<String> pairServIPs = readIDs(Config.INSTANCE.getPairService(), 1, Config.INSTANCE.getCurrentFeaturePath());
		Set<String> allIPs = netIPs;
		allIPs.retainAll(servIPs);
		allIPs.retainAll(timeIPs);
//		allIPs.retainAll(pairServIPs);
		
		mIDs = CUtil.makeList(allIPs);
		Collections.sort(mIDs);

		Log.log(Level.INFO, "Loaded {0}", mIDs.size() );
	}

	private void runInt() throws IOException {
		Config.INSTANCE.setFeatureDir(Config.INT_DIR);
		loadIntIPs();
		
		InstanceDatabase netflow = new Netflow(mIDs, Config.INSTANCE.getNetflow(), Config.INSTANCE.getNetflow()).run();
		InstanceDatabase ta = new TimeAccess(mIDs, Config.INSTANCE.getTimeSeries(), Config.INSTANCE.getTimeAccess()).run();
		
		InstanceDatabase baseInt = InstanceDatabase.mergeFeatures(Config.INSTANCE.getBasePath(),
				netflow,
				ta);

		baseInt.saveIPs();
		baseInt.writeReport();
		
		FileUtil.copy(baseInt.getIDPath(), Config.INSTANCE.getAggregatedNormPath() + WekaPreprocess.ID_SUFFIX);
		FileUtil.copy(baseInt.getARFFPath(), Config.INSTANCE.getAggregatedNormPath() + WekaPreprocess.ALL_SUFFIX);
		FileUtil.copy(baseInt.getReportARFFPath(), Config.INSTANCE.getAggregatedNormPath() + WekaPreprocess.REPORT_SUFFIX);
		InstanceDatabase baseIntNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
		baseIntNorm.normalize();
		baseIntNorm.writeReport();
	}

	private void loadIntIPs() throws IOException {
		Set<String> netIPs = readIDs(Config.INSTANCE.getNetflow(), 0, Config.INSTANCE.getCurrentFeaturePath());
		Set<String> timeIPs = readIDs(Config.INSTANCE.getTimeSeries(), 0, Config.INSTANCE.getCurrentFeaturePath());
		Set<String> allIPs = netIPs;
		allIPs.retainAll(timeIPs);
		
		mIDs = CUtil.makeList(allIPs);
		Collections.sort(mIDs);
		
		Log.log(Level.INFO, "Internal IP set = {0}", mIDs.size() );
	}
	
	private static Set<String> readIDs(String file, int ipIndex, String ... paths) throws IOException {
		Set<String> ips = CUtil.makeSet();
		for (String featurePath : paths) {
			BufferedReader in = new BufferedReader(new FileReader(featurePath + file));
			String line = in.readLine();		
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				ips.add(split[ipIndex]);
			}
			
			in.close();
		}
		
		Log.log(Level.INFO, "Read {0} from {1}", new Object[] {ips.size(), file} );
		return ips;
	}

}
