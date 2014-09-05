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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;


public class Config {

	private static final Logger Log = Logger.getLogger(Config.class.getName());
	
	private static final String[] CONFIG_FILES = new String[] {"scraper.conf"};
	
	public static final String MISSION_DOM_FILE = "crawler.mission.domains";
	public static final String NEW_LEXICAL_MISSION_SIM_FILE = "crawler.mission.sim";
	public static final String NEW_IP_DOM_FILE = "crawler.ip_dom";
	public static final String NEW_IP_WHOIS_FILE = "crawler.ip_whois";
	public static final String NEW_DOM_DOC_FILE = "crawler.dom_doc";
	
	public static final String MISSION_RAW_PATH =  "crawler.mission.raw" ;
	public static final String MISSION_OUTPUT_PATH = "crawler.mission.output";
	public static final String MISSION_MERGED_PATH = "crawler.mission.merged";
	public static final String WEB_RAW_PATH =  "crawler.web.raw" ;
	public static final String WEB_OUTPUT_PATH = "crawler.web.output";
	public static final String WEB_MERGED_PATH = "crawler.web.merged";
	public static final String MALLET_FILE = "crawler.web.mallet";
	
	public static final String CONCURRENT_CRAWLERS = "crawler.max_crawlers";
	public static final String CRAWLER_THREADS = "crawler.threads_per_crawler";
	public static final String MAX_PAGES_PER_HOST = "crawler.max_pages_per_host";
	public static final String POLITENESS_DELAY = "crawler.politeness_delay";
	
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

	private Config() throws ConfigurationException {
		CompositeConfiguration config = new CompositeConfiguration();
		for (String c : CONFIG_FILES) {
			config.addConfiguration(new PropertiesConfiguration(c));
		}
		mConfig = config;
//		Log.log(Level.INFO, "Loaded configuration: " + getConfigKeys());
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
	
}
