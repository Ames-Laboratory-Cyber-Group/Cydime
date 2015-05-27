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

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.NormalizeLog;
import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.ReplaceMissingValues;
import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.StringToNominal;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * We extract the number of bytes transferred for each hourly bin for each IP, and treat it as a sequence
 * of time series data. We then perform Discrete Fourier Transform on this time series and obtain the
 * coefficients of the corresponding daily frequency and weekly frequency.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class TimeSeries extends FeatureSet {
	
	private static class EpochConverter {
		
		private static final long BIN_SEC = 3600; 
		
		private long mMinEpoch = Long.MAX_VALUE;
		private long mMaxEpoch = Long.MIN_VALUE;
		
		public void updateRange(long time) {
			if (time < mMinEpoch) {
				mMinEpoch = time;
			}
			if (time > mMaxEpoch) {
				mMaxEpoch = time;
			}
		}

		public int getSeriesLength() {
			return (int) ((mMaxEpoch - mMinEpoch) / BIN_SEC + 1);
		}

		public int toIndex(long time) {
			return (int) ((time - mMinEpoch) / BIN_SEC);
		}
		
	}
	
	private static final Logger Log = Logger.getLogger(TimeSeries.class.getName());
	
	private static Converter[] CONVERTERS = new Converter[] {
		new FTConverter()
	};

	private static Set<String> SERIES = CUtil.asSet("Bytes");
	
	public static List<String> getRequiredPaths(String baseDir) {
		List<String> paths = CUtil.makeList();
		for (String s : SERIES) {
			paths.add(baseDir + Config.INSTANCE.getTimeSeries() + "." + s);
		}		
		return paths;
	}

	private EpochConverter mEpochConverter;
	private List<String> mSeries;
	private Map<String, String> mIDSeries;
	
	public TimeSeries(List<String> ids, String inPath, String outPath) {
		super(ids, inPath, outPath);
		mEpochConverter = new EpochConverter();
	}

	public InstanceDatabase run() throws IOException {
		readSchema();
		flatten();
		transform();
		mergeAllSeries();
		prepareInstances();
		
		WekaPreprocess.filterUnsuperivsed(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX,
				StringToNominal,
				ReplaceMissingValues,
				NormalizeLog);
		
		FileUtil.copy(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, mCurrentOutPath + WekaPreprocess.REPORT_SUFFIX);
		return new InstanceDatabase(mCurrentOutPath, mIDs);
	}
	
	private void readSchema() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(mCurrentInPath));
		String line = in.readLine();
		String[] split = StringUtil.trimmedSplit(line, ",");
		mSeries = CUtil.makeList();
		for (int i = 2; i < split.length; i++) {
			mSeries.add(split[i].trim());
		}
		
		Set<String> seenIDs = CUtil.makeSet();
		String lastID = "";
		while ((line = in.readLine()) != null) {
			line = line.trim();
			split = StringUtil.trimmedSplit(line, ",");
			long time = Long.parseLong(split[1]);
			mEpochConverter.updateRange(time);
			
			if (!lastID.equals(split[0])) {
				if (seenIDs.contains(split[0])) {
					in.close();
					throw new IllegalArgumentException("Error: Keys are not sorted in " + mCurrentInPath + " (" + split[0] + " has appeared already)");
				}
				lastID = split[0];
				seenIDs.add(split[0]);
			}
		}
		in.close();
		
//		System.out.println(seenIDs.size() + " " + mMinEpoch + "-" + mMaxEpoch + " " + (mMaxEpoch - mMinEpoch) / BIN_SEC);
	}

	private void flatten() throws IOException {
		final int LENGTH = mEpochConverter.getSeriesLength();
		
		Log.log(Level.INFO, "Processing time series (Phase 1)...");
		
		for (int i = 0; i < mSeries.size(); i++) {
			BufferedWriter out = new BufferedWriter(new FileWriter(mCurrentOutPath + "." + mSeries.get(i)));
			BufferedReader in = new BufferedReader(new FileReader(mCurrentInPath));
			
			long[] series = new long[LENGTH];
			out.write(String.valueOf(LENGTH));
			out.newLine();
			
			String lastID = null;
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				
				if (!split[0].equals(lastID)) {
					if (lastID != null) {
						writeSeries(out, lastID, series, true);
					}
					
					Arrays.fill(series, 0);
					lastID = split[0];
				}
				
				long time = Long.parseLong(split[1]);
				long value = Long.parseLong(split[i + 2]);
				series[mEpochConverter.toIndex(time)] = value;
			}
			
			if (lastID != null) {
				writeSeries(out, lastID, series, true);
			}
			
			in.close();
			out.close();
		}
	}

	private void writeSeries(BufferedWriter out, String id, long[] series, boolean sparse) throws IOException {
		out.write(id);
		for (int j = 0; j < series.length; j++) {
			if (sparse) {
				if (series[j] > 0) {
					out.write(",");
					out.write(j + ":" + series[j]);
				}
			} else {
				out.write(",");
				out.write(String.valueOf(series[j]));
			}
		}
		out.newLine();
	}
	
	public void transform() throws IOException {
		Log.log(Level.INFO, "Processing time series (Phase 2)...");
		
		for (int i = 0; i < mSeries.size(); i++) {
			if (SERIES.contains(mSeries.get(i))) {
				for (Converter c : CONVERTERS) {
					c.reset();
					c.learn(mPreprocessPaths, mSeries.get(i), mCurrentOutPath + "." + mSeries.get(i));
					c.convert(mPreprocessPaths, mSeries.get(i), mCurrentOutPath + "." + mSeries.get(i));
				}
			}
		}
	}

	private void mergeAllSeries() throws IOException {
		Log.log(Level.INFO, "Processing time series (Phase 3)...");
		
		mIDSeries = CUtil.makeMap();
		for (int i = 0; i < mSeries.size(); i++) {
			if (SERIES.contains(mSeries.get(i))) {
				for (int j = 0; j < CONVERTERS.length; j++) {
					mergeOneSeries(mCurrentOutPath + "." + mSeries.get(i) + "." + CONVERTERS[j].getName());
				}
			}
		}
	}

	private void mergeOneSeries(String file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = in.readLine()) != null) {
			int comma = line.indexOf(",");
			String id = line.substring(0, comma);
			line = line.substring(comma + 1);
			String series = mIDSeries.get(id);
			if (series == null) {
				series = line;
			} else {
				series = new StringBuilder(series).append(",").append(line).toString();
			}
			mIDSeries.put(id, series);
		}
		in.close();
	}

	private void prepareInstances() throws IOException {
		List<String> atts = CUtil.makeList();
		for (int i = 0; i < mSeries.size(); i++) {
			if (SERIES.contains(mSeries.get(i))) {
				for (int j = 0; j < CONVERTERS.length; j++) {
					int K = CONVERTERS[j].getSize();
					for (int k = 0; k < K; k++) {
						atts.add(mSeries.get(i) + "_" + CONVERTERS[j].getName() + k + " numeric");
					}
				}
			}
		}
		
		ARFFWriter out = new ARFFWriter(mCurrentOutPath + WekaPreprocess.ALL_SUFFIX, "timeseries", null, atts); 
		
		for (String id : mIDs) {
			String series = mIDSeries.get(id);
			out.writeLine(series + ",?");
		}
		
		out.close();
	}

}
