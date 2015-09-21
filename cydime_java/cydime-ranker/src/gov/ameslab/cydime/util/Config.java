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

import gov.ameslab.cydime.preprocess.WekaPreprocess;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class Config {

	private static final Logger Log = Logger.getLogger(Config.class.getName());
	
	private static final String CONFIG_FILE = "cydime.conf";
	
	public static final String SILK_SERVICE_DIR = "silk_map_dir";
	public static final String ANALYSIS_INTERVAL = "days_to_analyze";
	public static final String DATA_DIR = "data_dir";
	public static final String FEATURE_DIR = "features.dir";
	public static final String PREPROCESS_DIR = "preprocess.dir";
	public static final String MODEL_DIR = "model.dir";
	public static final String REPORT_DIR = "report.dir";
	public static final String FEATURE_LABEL_FILE = "label.feature";
	public static final String LEXICAL_MISSION_SIM_FILE = "lexical.mission.sim";
	public static final String MISSION_SIM_THRESHOLD = "lexical.mission.threhold";
	public static final String STATIC_WHITE_FILE = "label.static.white";
	public static final String STATIC_BLACK_FILE = "label.static.black";
	
	public static final String IP_DIR = "ip/";
	public static final String ASN_DIR = "asn/";
	public static final String INT_DIR = "int/";

	public static final DateFormat FORMAT_DATE = new SimpleDateFormat("yyyy/MM/dd");
	
	public static final Config INSTANCE;

	
	
	static {
		try {
			INSTANCE = new Config();
		} catch (ConfigurationException e) {
			Log.log(Level.SEVERE, "Error loading configuration: " + e);
			throw new RuntimeException(e);
		}
	}

	
	private Configuration mConfig; 
	private String mRootPath;
	private String mCurrentDatePath;
	private String[] mFeaturePaths; 
	private String[] mPreprocessPaths;
	private String mFeatureSet;
		
	private Config() throws ConfigurationException {
		mConfig = new PropertiesConfiguration(CONFIG_FILE);
//		Log.log(Level.INFO, "Loaded configuration: " + getConfigKeys());
		
		mRootPath = getPath(DATA_DIR);
	}

	private List<String> getConfigKeys() {
		List<String> keys = CUtil.makeList();
		for (Iterator<String> it = mConfig.getKeys(); it.hasNext(); ) {
			keys.add(it.next());
		}
		Collections.sort(keys);
		return keys;
	}

	public int getInt(String key) {
		if (!mConfig.containsKey(key)) throw new IllegalArgumentException("Error: Key not found in configuration: " + key);
		return mConfig.getInt(key);
	}
	
	public String getString(String key) {
		if (!mConfig.containsKey(key)) throw new IllegalArgumentException("Error: Key not found in configuration: " + key);
		return mConfig.getString(key);
	}
	
	public String getPath(String key) {
		return StringUtil.appendSlash(getString(key));
	}
	
	public void setParam(String datePath) {
		mCurrentDatePath = StringUtil.appendSlash(datePath);
	}

	public String getRootPath() {
		return mRootPath;
	}
	
	public String getCurrentDatePath() {
		return mCurrentDatePath;
	}
	
	public String getRootDatePath() {
		return mRootPath + mCurrentDatePath;
	}
	
	public void setFeatureDir(String dir) {
		mFeatureSet = dir;
		
		List<Date> dates = CUtil.makeList();
		if (IP_DIR.equals(mFeatureSet)) {
			dates = findValidDates(getFeatureIPPaths());
		} else if (ASN_DIR.equals(mFeatureSet)) {
			dates = findValidDates(getFeatureASNPaths());
		} else if (INT_DIR.equals(mFeatureSet)) {
			dates = findValidDates(getFeatureIntPaths());
		}
		
		List<String> formattedDates = CUtil.makeList();
		for (Date d : dates) {
			formattedDates.add(FORMAT_DATE.format(d));
		}
		Log.log(Level.INFO, "Found valid feature dates: {0}", formattedDates.toString() );
		
		mFeaturePaths = new String[dates.size()];
		for (int i = 0; i < mFeaturePaths.length; i++) {
			mFeaturePaths[i] = mRootPath + FORMAT_DATE.format(dates.get(i)) + "/" + getPath(FEATURE_DIR) + mFeatureSet;
		}
		
		mPreprocessPaths = new String[dates.size()];
		for (int i = 0; i < mPreprocessPaths.length; i++) {
			mPreprocessPaths[i] = mRootPath + FORMAT_DATE.format(dates.get(i)) + "/" + getPath(PREPROCESS_DIR) + mFeatureSet;
		}
	}

	public List<String> getFeatureIPPaths() {
		return Arrays.asList(
				getPath(FEATURE_DIR) + IP_DIR + getService(),
				getPath(FEATURE_DIR) + IP_DIR + getNetflow(),
				getPath(FEATURE_DIR) + IP_DIR + getTimeSeries(),
				getPath(FEATURE_DIR) + IP_DIR + getDailyProfile(),
				getPath(FEATURE_DIR) + IP_DIR + getServiceTimeSeries(),	//Explorer
				getPath(FEATURE_DIR) + IP_DIR + getPairService()			//Explorer
		);
	}
	
	public List<String> getFeatureASNPaths() {
		return Arrays.asList(
				getPath(FEATURE_DIR) + ASN_DIR + getService(),
				getPath(FEATURE_DIR) + ASN_DIR + getNetflow(),
				getPath(FEATURE_DIR) + ASN_DIR + getTimeSeries(),
				getPath(FEATURE_DIR) + ASN_DIR + getDailyProfile(),
				getPath(FEATURE_DIR) + ASN_DIR + getServiceTimeSeries(),	//Explorer
				getPath(FEATURE_DIR) + ASN_DIR + getPairService()			//Explorer
		);
	}

	public List<String> getFeatureIntPaths() {
		return Arrays.asList(
				getPath(FEATURE_DIR) + INT_DIR + getService(),
				getPath(FEATURE_DIR) + INT_DIR + getNetflow(),
				getPath(FEATURE_DIR) + INT_DIR + getTimeSeries(),
				getPath(FEATURE_DIR) + INT_DIR + getServiceTimeSeries()
		);
	}
	
	public List<String> getModelIPPaths() {
		return Arrays.asList(
				getPath(MODEL_DIR) + IP_DIR + getDailyFile() + WekaPreprocess.ID_SUFFIX,
				getPath(MODEL_DIR) + IP_DIR + getDailyFile() + WekaPreprocess.ALL_SUFFIX,
				getPath(MODEL_DIR) + IP_DIR + getDailyFile() + WekaPreprocess.REPORT_SUFFIX
		);
	}

	public List<Date> findValidDates(List<String> pathsToCheck) {
		return findValidDates(pathsToCheck, mCurrentDatePath);
	}
	
	public List<Date> findValidDates(List<String> pathsToCheck, int daysToAnalyze) {
		return findValidDates(pathsToCheck, mCurrentDatePath, daysToAnalyze);
	}
	
	public List<Date> findValidDates(List<String> pathsToCheck, String datePath) {
		return findValidDates(pathsToCheck, datePath, getInt(ANALYSIS_INTERVAL));
	}
	
	public List<Date> findValidDates(List<String> pathsToCheck, String datePath, int daysToAnalyze) {
		Date endDate = null;
		try {
			endDate = FORMAT_DATE.parse(datePath.substring(0, datePath.length() - 1));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(endDate);
		
		List<Date> dates = CUtil.makeList();
		
		int weekdays = 0;
		if (isWeekday(cal, endDate)) {
			dates.add(endDate);
			weekdays++;
		}
		
		while (weekdays < daysToAnalyze) {
			cal.add(Calendar.DAY_OF_YEAR, -1);
			Date date = cal.getTime();
			if (isWeekday(cal, date)) {
				String dateStr = FORMAT_DATE.format(date);
				if (isPathValid(dateStr, pathsToCheck)) {
					dates.add(date);
					weekdays++;
				} else break;
			}
		}
		Collections.reverse(dates);
		return dates;
	}

	private static boolean isWeekday(Calendar cal, Date date) {
		int d = cal.get(Calendar.DAY_OF_WEEK);
		return d != Calendar.SATURDAY && d != Calendar.SUNDAY;
	}

	private boolean isPathValid(String dateStr, List<String> pathsToCheck) {
		String path = mRootPath + dateStr + "/";
		for (String pathToCheck : pathsToCheck) {
			File f = new File(path + pathToCheck);
			if (!f.exists()) {
				Log.log(Level.INFO, "File not found, skipping date: {0}", f.toString());
				return false;
			}
		}
		return true;
	}
	
	public String getCurrentFeaturePath() {
		return getRootDatePath() + getPath(FEATURE_DIR) + mFeatureSet;
	}
	
	public String[] getFeaturePaths() {
		return mFeaturePaths;
	}
	
	public String getCurrentPreprocessPath() {
		return getRootDatePath() + getPath(PREPROCESS_DIR) + mFeatureSet;
	}
	
	public String[] getPreprocessPaths() {
		return mPreprocessPaths;
	}
	
	public String getCurrentModelPath() {
		return getRootDatePath() + getPath(MODEL_DIR) + mFeatureSet;
	}
	
	public String getCurrentReportPath() {
		return getRootDatePath() + getPath(REPORT_DIR) + mFeatureSet;
	}
	
	
	//getFeaturePath
	public String getDailyProfile() {	return "pair_services_timeseries.features";	}
	public String getService() {	return "services.features";	}
	public String getNetflow() {	return "netflow.features";	}
	public String getTimeSeries() {	return "timeseries.features";	}
	public String getTimeAccess() {	return "timeseries.features.access";	}
	public String getServiceTimeSeries() {	return "services_timeseries.features";	}
	public String getPairService() {	return "pair_services.features";	}
	public String getLexical() {	return "lexical.features";	}
	
	//getPreprocessPath
	public String getHierarchy() {	return "hierarchy.features";	}
	public String getIPHostMapPath() {	return getRootDatePath() + getPath(PREPROCESS_DIR) + "ipHostMap.csv";	}
	public String getIPASNMapPath() {	return getRootDatePath() + getPath(PREPROCESS_DIR) + "ipASNMap.csv";	}
	public String getCommunityPath() {	return getCurrentPreprocessPath() + "pair_services.features.bicom.csv";	}
	public String getExtIntGraphPath() {	return getCurrentPreprocessPath() + "pair_services.features.graph.csv";	}

	public String getBasePath() {	return getCurrentPreprocessPath() + "base";	}
	public String getDerivedPath() {	return getCurrentPreprocessPath() + "derived";	}

	//getModelPath
	public String getDailyFile() {	return "daily";	}
	public String getDailyPath() {	return getCurrentModelPath() + getDailyFile();	}
	public String getAggregatedPath() {	return getCurrentModelPath() + "aggregated";	}
	public String getAggregatedNormPath() {	return getCurrentModelPath() + "aggregated.norm";	}
	public String getModelPath() {	return getCurrentModelPath() + "model";	}
	public String getRankScorePath() {	return getCurrentModelPath() + "rank_score";	}
	
	//getReportPath
	public String getFinalResultPath() {	return getCurrentReportPath() + "cydime.scores";	}
	
	public String getSilkServicePath() {	return getPath(SILK_SERVICE_DIR) + "services.txt";	}

}
