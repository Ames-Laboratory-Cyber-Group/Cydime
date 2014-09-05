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

import gov.ameslab.cydime.preprocess.timeseries.TimeSeries;

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
	public static final String LEXICAL_MISSION_SIM_FILE = "lexical.mission.sim";
	public static final String IP_DOM_FILE = "label.ip_dom";
	public static final String IP_WHOIS_FILE = "label.ip_whois";
	public static final String DOM_DOC_FILE = "label.dom_doc";
	
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

	public String getRootPath() {
		return mRootPath;
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
		searchFeaturePaths();
	}
	
	private void searchFeaturePaths() {
		DateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		List<String> features = CUtil.makeList();
		features.addAll(Arrays.asList(
				getPath(FEATURE_DIR) + getService(),
				getPath(FEATURE_DIR) + getNetflow(),
				getPath(FEATURE_DIR) + getTimeSeries(),
				getPath(FEATURE_DIR) + getServiceTimeSeries(),
				getPath(FEATURE_DIR) + getPairService(),
				getPath(FEATURE_DIR) + getIntService(),
				getPath(FEATURE_DIR) + getIntNetflow(),
				getPath(FEATURE_DIR) + getIntTimeSeries(),
				getPath(FEATURE_DIR) + getIntServiceTimeSeries()
		));
		features.addAll(TimeSeries.getRequiredPaths());
		
		Date currentDate = null;
		try {
			currentDate = format.parse(mCurrentDatePath.substring(0, mCurrentDatePath.length() - 1));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(currentDate);
		List<Date> dates = CUtil.makeList();
		dates.add(currentDate);
		for (int i = 1; i < getInt(ANALYSIS_INTERVAL); i++) {
			cal.add(Calendar.DAY_OF_YEAR, -1);
			Date date = cal.getTime();
			String dateStr = format.format(date);
			if (validFeaturePath(dateStr, features)) {
				dates.add(date);
			} else break;
		}
		Collections.reverse(dates);
		
		List<String> formattedDates = CUtil.makeList();
		for (Date d : dates) {
			formattedDates.add(format.format(d));
		}
		Log.log(Level.INFO, "Found valid feature dates: {0}", formattedDates.toString() );
		
		mFeaturePaths = new String[dates.size()];
		for (int i = 0; i < mFeaturePaths.length; i++) {
			mFeaturePaths[i] = mRootPath + format.format(dates.get(i)) + "/" + getPath(FEATURE_DIR);
		}
		
		mPreprocessPaths = new String[dates.size()];
		for (int i = 0; i < mPreprocessPaths.length; i++) {
			mPreprocessPaths[i] = mRootPath + format.format(dates.get(i)) + "/" + getPath(PREPROCESS_DIR);
		}
	}

	private boolean validFeaturePath(String dateStr, List<String> features) {
		String path = mRootPath + dateStr + "/";
		for (String feature : features) {
			File f = new File(path + feature);
			if (!f.exists()) {
				Log.log(Level.INFO, "File not found, skipping date: {0}", f.toString());
				return false;
			}
		}
		return true;
	}
	
	public String getCurrentDatePath() {
		return mCurrentDatePath;
	}
	
	public String getCurrentFeaturePath() {
		return mRootPath + mCurrentDatePath + getPath(FEATURE_DIR);
	}
	
	public String[] getFeaturePaths() {
		return mFeaturePaths;
	}
	
	public String getCurrentPreprocessPath() {
		return mRootPath + mCurrentDatePath + getPath(PREPROCESS_DIR);
	}
	
	public String[] getPreprocessPaths() {
		return mPreprocessPaths;
	}
	
	public String getCurrentModelPath() {
		return mRootPath + mCurrentDatePath + getPath(MODEL_DIR);
	}
	
	public String getCurrentReportPath() {
		return mRootPath + mCurrentDatePath + getPath(REPORT_DIR);
	}
	
	
	//getFeaturePath
	public String getDailyProfile() {	return "pair_services_timeseries.ext.features";	}
	public String getService() {	return "services.ext.features";	}
	public String getNetflow() {	return "full_netflow.ext.features";	}
	public String getTimeSeries() {	return "timeseries.ext.features";	}
	public String getTimeAccess() {	return "timeseries.ext.features.access";	}
	public String getServiceTimeSeries() {	return "services_timeseries.ext.features";	}
	public String getPairService() {	return "pair_services.features";	}
	public String getLexical() {	return "lexical.features";	}
	
	public String getIntService() {	return "services.int.features";	}
	public String getIntNetflow() {	return "full_netflow.int.features";	}
	public String getIntTimeSeries() {	return "timeseries.int.features";	}
	public String getIntTimeAccess() {	return "timeseries.int.features.access";	}
	public String getIntServiceTimeSeries() {	return "services_timeseries.int.features";	}
	
	//getPreprocessPath
	public String getHierarchy() {	return "hierarchy.features";	}
	public String getCommunityPath() {	return getCurrentPreprocessPath() + "pair_services.features.bicom.csv";	}
	public String getExtIntGraphPath() {	return getCurrentPreprocessPath() + "pair_services.features.graph.csv";	}
	
	//getModelPath
	public String getBasePath() {	return getCurrentModelPath() + "base.ext";	}
	public String getBaseNormPath() {	return getCurrentModelPath() + "base.ext.norm";	}
	public String getBaseScorePath() {	return getCurrentModelPath() + "base_score";	}
	public String getStackPath() {	return getCurrentModelPath() + "stack.ext";	}
	
	//getReportPath
	public String getIntBasePath() {	return getCurrentReportPath() + "base.int";	}
	public String getIntBaseNormPath() {	return getCurrentReportPath() + "base.int.norm";	}
	public String getFinalResultPath() {	return getCurrentReportPath() + "cydime.scores";	}
	public String getLexicalSimPath() {	return getCurrentReportPath() + "lexical_sim";	}
	public String getSemanticPath() {	return getCurrentReportPath() + "semantic";	}
	public String getStrengthPath() {	return getCurrentReportPath() + "strength";	}
	public String getAllPredictedPath() {	return getCurrentReportPath() + "predicted";	}
	
	public String getSilkServicePath() {	return getPath(SILK_SERVICE_DIR) + "services.txt";	}
	
}
