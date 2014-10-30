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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;


public class Config {

	private static final Logger Log = Logger.getLogger(Config.class.getName());
	
	public static final String ACTIVE_INTERNAL_FILE = "preprocess/int.csv";
	public static final String ACTIVE_EXTERNAL_FILE = "preprocess/ext.csv";
	public static final String IP_WHOIS_FILE = "preprocess/IPWhoisMap.csv";
	public static final String IP_NETREG_FILE = "preprocess/netreg_nobuilding.csv";
	public static final String BIGRAPH_PATH = "bigraph/";
	
	private static final String CONFIG_FILE = "cydime.conf";
	
	public static final String SILK_SERVICE_DIR = "silk_map_dir";
	public static final String DATA_DIR = "data_dir";
	public static final String IP_DOM_FILE = "label.ip_dom";
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
		
	public String getCurrentDatePath() {
		return mCurrentDatePath;
	}
		
	public String getSilkServicePath() {	return getPath(SILK_SERVICE_DIR) + "services.txt";	}
	
}
