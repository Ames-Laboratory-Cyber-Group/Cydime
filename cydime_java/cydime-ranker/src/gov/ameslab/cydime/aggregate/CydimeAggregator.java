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

package gov.ameslab.cydime.aggregate;

import gov.ameslab.cydime.aggregate.aggregator.MeanAggregator;
import gov.ameslab.cydime.aggregate.aggregator.ModeAggregator;
import gov.ameslab.cydime.aggregate.aggregator.VarianceAggregator;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.HistogramLong;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Cydime Aggregator.
 * Aggregate Config.ANALYSIS_INTERVAL days of daily preprocessed features via means and variance.
 * - Every IP has to occur more than MIN_DAYS days
 * - Aggregation for an IP ranges over the first day of occurrence until the current day, an empty occurrence in between this range counts as 0 value for each feature
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeAggregator {
	
	private static final Logger Log = Logger.getLogger(CydimeAggregator.class.getName());
	
	private final int MIN_DAYS = 2;
	
	private int mAnalysisDays;
	private String mIDPath;
	
	private String[] mModelPaths;
	private List<String> mIDs;
	
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			new CydimeAggregator(args[0]).run();
		} else if (args.length == 2) {
			new CydimeAggregator(args[0], args[1]).run();
		} else if (args.length == 3) {
			new CydimeAggregator(args[0], args[1], args[2]).run();
		} else {
			printUsage();
			return;
		}
	}

	private static void printUsage() {
		System.out.println("[java] CydimeAggregator FEATURE_DIR [DAYS_INTERNAL] [ID_PATH]");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
		System.out.println("    DAYS_INTERNAL: Optional. Number of days to analyze.");
		System.out.println("    ID_PATH: Optional. File path containing a list of IDs to override.");
	}
	
	public CydimeAggregator(String datePath) throws IOException {
		this(datePath, null);
	}
	
	public CydimeAggregator(String datePath, String days) throws IOException {
		this(datePath, days, null);
	}
	
	public CydimeAggregator(String datePath, String days, String idPath) throws IOException {
		Config.INSTANCE.setParam(datePath);
		
		mAnalysisDays = -1;
		if (days != null) {
			try {
				mAnalysisDays = Integer.parseInt(days);
				
				Log.log(Level.INFO, "Using DAYS_INTERNAL = {0}", mAnalysisDays);
			} catch (NumberFormatException ex) {			
			}
		}
		
		if (idPath != null) {
			mIDPath = idPath;
			Log.log(Level.INFO, "Using ID_PATH = {0}", mIDPath);
		}
	}
	
	private void run() {
		try {
			runExtIP();
		} catch (IOException e) {
			Log.log(Level.SEVERE, e.toString());
			e.printStackTrace();
		}
	}
	
	private void runExtIP() throws IOException {
		Config.INSTANCE.setFeatureDir(Config.IP_DIR);
		
		findValidModelPaths();
		loadAndFilterIDs();
		InstanceDatabase aggregate = aggregate(getSchema());
		
		Log.log(Level.INFO, "Normalizing...");
		
		//Save as report
		FileUtil.copy(aggregate.getIDPath(), Config.INSTANCE.getAggregatedNormPath() + WekaPreprocess.ID_SUFFIX);
		FileUtil.copy(aggregate.getARFFPath(), Config.INSTANCE.getAggregatedNormPath() + WekaPreprocess.ALL_SUFFIX);
		FileUtil.copy(aggregate.getReportARFFPath(), Config.INSTANCE.getAggregatedNormPath() + WekaPreprocess.REPORT_SUFFIX);
		InstanceDatabase aggregateNorm = InstanceDatabase.load(Config.INSTANCE.getAggregatedNormPath());
		//Normalize
		aggregateNorm.normalize();
		aggregateNorm.writeReport();
	}

	private void findValidModelPaths() {
		List<Date> dates = null;
		if (mAnalysisDays > 0) {
			dates = Config.INSTANCE.findValidDates(Config.INSTANCE.getModelIPPaths(), mAnalysisDays);
		} else {
			dates = Config.INSTANCE.findValidDates(Config.INSTANCE.getModelIPPaths());
		}
		
		List<String> formattedDates = CUtil.makeList();
		for (Date d : dates) {
			formattedDates.add(Config.FORMAT_DATE.format(d));
		}
		Log.log(Level.INFO, "Found valid model dates: {0}", formattedDates.toString() );
		
		mModelPaths = new String[dates.size()];
		for (int i = 0; i < mModelPaths.length; i++) {
			mModelPaths[i] = Config.INSTANCE.getRootPath() + Config.FORMAT_DATE.format(dates.get(i)) + "/" + Config.INSTANCE.getPath(Config.MODEL_DIR) + Config.IP_DIR;
		}
	}

	private void loadAndFilterIDs() throws IOException {
		if (mIDPath == null) {
			HistogramLong<String> idCount = new HistogramLong<String>();
			for (String path : mModelPaths) {
				InstanceDatabase daily = InstanceDatabase.load(path + Config.INSTANCE.getDailyFile());
				for (String id : daily.getIDs()) {
					idCount.increment(id);
				}
			}
			
			mIDs = CUtil.makeList();
			for (Entry<String, Long> entry : idCount.entrySet()) {
				if (entry.getValue() >= MIN_DAYS) {
					mIDs.add(entry.getKey());
				}
			}
			Collections.sort(mIDs);
			
		} else {
			mIDs = FileUtil.readFile(mIDPath);
		}

		Log.log(Level.INFO, "Loaded {0}", mIDs.size() );
	}

	private Instances getSchema() throws IOException {
		InstanceDatabase schema = InstanceDatabase.load(mModelPaths[0] + Config.INSTANCE.getDailyFile());
		return new Instances(schema.getWekaInstances(), 0);
	}

	private InstanceDatabase aggregate(Instances schema) throws IOException {
		Log.log(Level.INFO, "Loading daily preprocessed data...");
		
		Set<String> idSet = CUtil.makeSet(mIDs);
		InstancesAggregator agg = new InstancesAggregator(mIDs, schema,
				Arrays.asList(
						new ModeAggregator(),
						new MeanAggregator(),
						new VarianceAggregator()
				));
				
		for (String path : mModelPaths) {
			InstanceDatabase daily = InstanceDatabase.load(path + Config.INSTANCE.getDailyFile());
			for (String id : daily.getIDs()) {
				if (!idSet.contains(id)) continue;
				
				Instance inst = daily.getWekaInstance(id);
				agg.addInstance(id, inst);
			}
			
			Set<String> missingIDs = CUtil.makeSet(mIDs);
			missingIDs.removeAll(daily.getIDs());
			for (String id : missingIDs) {
				agg.addZeroInstance(id);
			}
		}
		
		return agg.aggregate();
	}

}
