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

package gov.ameslab.cydime.simulate;

import gov.ameslab.cydime.model.ListDatabase;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cydime Predictor experimenter using a set of representative predictors.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeResponseExp {

	private static final Logger Log = Logger.getLogger(CydimeResponseExp.class.getName());

	private static final long[] COMPLAINT = {5 * 60, 10 * 60, 20 * 60, 40 * 60};
	private static final int INC_ANALYSIS = 30;
	private static final int MAX_ANALYSIS = 1200;
	private static final AnalysisBuffer[] BUFFER_PROTOTYPE = {new FIFOBuffer(), new LIFOBuffer(), new SortedBuffer()};
	private static final DecimalFormat FORMAT = new DecimalFormat("0.0000");
	
	private String mAlertFile;
	private String mAlertName;
	private boolean mDoWhitelist;
	
	private Map<String, Double> mIPScore;
	private AlertStream mAlerts;
	private ComplaintFirstBuffer mBuffer;
	private Set<String> mStatic;
	private Set<String> mTrueWhite;

	
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 3) {
			printUsage();
			return;
		}

		new CydimeResponseExp(args).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimeResponseExp FEATURE_DIR ALERT_FILE [-W]");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
		System.out.println("    ALERT_FILE: path specifying sorted alert CSV file");
		System.out.println("    -W: Optional, print a list of alerted IPs that are part of whitelist");
	}

	public CydimeResponseExp(String[] args) {
		Config.INSTANCE.setParam(args[0]);
		Config.INSTANCE.setFeatureDir(Config.IP_DIR);
		mAlertFile = args[1];
		mAlertName = mAlertFile.substring(0, mAlertFile.indexOf("."));
		if (args.length == 3 && args[2].equalsIgnoreCase("-W")) {
			mDoWhitelist = true;
		}
	}

	private void run() throws Exception {
		ListDatabase whiteDB = ListDatabase.read(Config.INSTANCE.getString(Config.STATIC_WHITE_FILE));
		
		readScore();
		mAlerts = new AlertStream(mAlertFile, mIPScore);
		mStatic = CUtil.makeSet();
		mTrueWhite = whiteDB.getSet(mAlerts.getIPs());
		
		Log.log(Level.INFO, "TrueWhite = {0}", mTrueWhite.size());
		
		if (mDoWhitelist) {
			Log.log(Level.INFO, "Printing alert whitelist...");
			
			BufferedWriter out = new BufferedWriter(new FileWriter("alert_white.txt"));
			List<List<String>> list = whiteDB.getList(CUtil.makeList(mAlerts.getIPs()));
			for (List<String> l : list) {
				out.write(l.toString());
				out.newLine();
			}
			out.close();
			
			return;
		}
		
		SimulationResult[] results = new SimulationResult[BUFFER_PROTOTYPE.length];
		for (int i = 0; i < results.length; i++) {
			results[i] = new SimulationResult();
		}
		
		for (long comp : COMPLAINT) {
			Log.log(Level.INFO, "Simulating with complaint threshold = {0}...", comp);
			
			BufferedWriter out = new BufferedWriter(new FileWriter(mAlertName + "-comp" + comp + ".csv"));
			out.write("Analysis Time");
			for (AnalysisBuffer proto : BUFFER_PROTOTYPE) {
				out.write(",complaint-");
				out.write(proto.getName());
			}
			for (AnalysisBuffer proto : BUFFER_PROTOTYPE) {
				out.write(",threshold-");
				out.write(proto.getName());
			}
			out.newLine();
			
			for (int analysis = INC_ANALYSIS; analysis <= MAX_ANALYSIS; analysis += INC_ANALYSIS) {		
				for (int s = 0; s < BUFFER_PROTOTYPE.length; s++) {
					mBuffer = new ComplaintFirstBuffer(BUFFER_PROTOTYPE[s].makeNew(), mTrueWhite, comp);
					mAlerts.reset();
					mStatic.clear();
					runSimulation(results[s], mBuffer.getName(), comp, analysis);					
				}
				
				out.write(FORMAT.format(analysis / 60.0));
				for (int s = 0; s < results.length; s++) {
					out.write(",");
					out.write(FORMAT.format(results[s].getComplaintCount() * 100.0 / mTrueWhite.size()));
				}
				for (int s = 0; s < results.length; s++) {
					out.write(",");
					out.write(FORMAT.format(results[s].getThresholdCount() * 100.0 / mAlerts.getIPs().size()));
				}
				out.newLine();
			}
			
			out.close();
		}	
	}

	private void runSimulation(SimulationResult result, String name, long comp, int analysis) {
		Log.log(Level.INFO, "Simulating with " + name + " Comp=" + comp + " Analysis=" + analysis + " ...");
		
		int compCount = 0;
		int exceedsThresholdCount = 0;
		for (long clock = 0; !mAlerts.isEmpty() || !mBuffer.isEmpty(); clock += analysis) {
			//Stream new alerts into buffer
			List<Alert> alerts = mAlerts.advance(clock);
			if (!alerts.isEmpty()) {
				mBuffer.insert(alerts);
			}
			
			//Pop top alert from buffer
			Alert a = null;
			while (true) {
				a = mBuffer.pop(clock);
				if (a == null) break;
				
				if (!mStatic.contains(a.getIP())) break;
			}
			
			if (a == null) continue;
			
			//Check complaint
			if (a.exceedsThreshold(clock, comp)) {
				exceedsThresholdCount++;
				
				if (a.isMemberOf(mTrueWhite)) {
					compCount++;
				}					
			}
			
			mStatic.add(a.getIP());
		}
		
		result.setComplaintCount(compCount);
		result.setThresholdCount(exceedsThresholdCount);
	}

	private void readScore() throws IOException {
		mIPScore = CUtil.makeMap();
		Map<String, String> ipScore = FileUtil.readCSV(Config.INSTANCE.getFinalResultPath(), 0, 1, false, true);
		for (Entry<String, String> entry : ipScore.entrySet()) {
			double v = Double.parseDouble(entry.getValue());
			mIPScore.put(entry.getKey(), v);
		}
	}

}
