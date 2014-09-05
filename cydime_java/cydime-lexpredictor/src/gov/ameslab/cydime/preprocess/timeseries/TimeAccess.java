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

package gov.ameslab.cydime.preprocess.timeseries;

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.ReplaceMissingValues;
import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.StringToNominal;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.Histogram;
import gov.ameslab.cydime.util.MapSet;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeAccess extends FeatureSet {

	private static final Logger Log = Logger.getLogger(TimeAccess.class.getName());
	
	public static int WORK_BEGIN_HOUR = 8;
	public static int WORK_END_HOUR = 17;
	private static final DecimalFormat FORMAT = new DecimalFormat("0.000");

	private Calendar mCal = GregorianCalendar.getInstance();

	public TimeAccess(List<String> ipList, String inPath, String outPath) {
		super(ipList, inPath, outPath);
	}

	private int getDayOfYear(Date date) {
		mCal.setTime(date);
		return mCal.get(Calendar.DAY_OF_YEAR);
	}
	
	private int getHourOfDay(Date date) {
		mCal.setTime(date);
		return mCal.get(Calendar.HOUR_OF_DAY);
	}
	
	public InstanceDatabase run() throws IOException {
		Log.log(Level.INFO, "Processing time access...");
		
		Set<String> allIPs = CUtil.makeSet();
		MapSet<Integer, String> daySet = new MapSet<Integer, String>();
		Histogram<String> onWorkHours = new Histogram<String>();
		Histogram<String> offWorkHours = new Histogram<String>();
		
		for (String inPath : mFeaturePaths) {
			BufferedReader in = new BufferedReader(new FileReader(inPath));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				String ip = split[0];
				allIPs.add(ip);
				long epoch = Long.parseLong(split[1]) * 1000;
				Date date = new Date(epoch);
				int dayOfYear = getDayOfYear(date);
				daySet.add(dayOfYear, ip);
				int hourOfDay = getHourOfDay(date);
				if (hourOfDay >= WORK_BEGIN_HOUR && hourOfDay <= WORK_END_HOUR) {
					onWorkHours.increment(ip);
				} else {
					offWorkHours.increment(ip);
				}
			}
			in.close();
		}
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "timeaccess", null,
				"access_hours numeric",
				"access_days numeric",
				"workhour_perc numeric"
				); 
		
		Set<Integer> days = daySet.keySet();
		for (String ip : mAllIPs) {
			int accessDays = 0;
			for (int day : days) {
				if (daySet.contains(day, ip)) {
					accessDays++;
				}
			}
			
			int accessHours = (int) (onWorkHours.get(ip) + offWorkHours.get(ip));
			double workhour_perc = onWorkHours.get(ip) / accessHours;
			
			out.writeValues(String.valueOf(accessHours),
					String.valueOf(accessDays),
					FORMAT.format(workhour_perc),
					"?");
		}
		
		out.close();
		
		WekaPreprocess.filterUnsuperivsed(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX,
				StringToNominal,
				ReplaceMissingValues);
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		
		return new InstanceDatabase(mCurrentOutPath, mAllIPs);
	}
	
}
