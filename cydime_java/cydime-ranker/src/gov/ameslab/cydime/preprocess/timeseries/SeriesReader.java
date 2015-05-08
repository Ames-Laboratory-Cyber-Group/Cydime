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

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.MathUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class SeriesReader {

	private String[] mInPaths;
	private String mInFile;
	private int mHoursPerFile;
	
	private List<String> mAllIPs;
	private int mCursor;
	private String mCursorIP;
	private String[][] mLineSplits;
	private BufferedReader[] mIns;
	private double[] cSeries;
	
	public SeriesReader(String[] inPaths, String inFile, int hoursPerFile) throws IOException {
		mInPaths = inPaths;
		mInFile = inFile;
		mHoursPerFile = hoursPerFile;
		init();
	}

	private void init() throws IOException {
		Set<String> allIPs = CUtil.makeSet();
		for (int i = 0; i < mInPaths.length; i++) {
			String lastIP = "";
			String file = mInPaths[i] + "." + mInFile;
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = in.readLine();
			while ((line = in.readLine()) != null) {
				String[] split = StringUtil.trimmedSplit(line, ",");
				String ip = split[0];
				allIPs.add(ip);
				if (lastIP.compareTo(ip) > 0) {
					in.close();
					throw new RuntimeException("Error: IPs are not sorted in " + file + " " + ip + " < " + lastIP);
				} else {
					lastIP = ip;
				}
			}
			in.close();
		}
		
		mAllIPs = CUtil.makeList(allIPs);
		Collections.sort(mAllIPs);
		mLineSplits = new String[mInPaths.length][];
		mIns = new BufferedReader[mInPaths.length];
		for (int i = 0; i < mInPaths.length; i++) {
			mIns[i] = new BufferedReader(new FileReader(mInPaths[i] + "." + mInFile));
			mIns[i].readLine();
			readLine(i);
		}
		mCursor = 0;
		cSeries = new double[getLength()];
	}

	private void readLine(int i) throws IOException {
		if (mIns[i] == null) return;
		
		String line = mIns[i].readLine();
		if (line == null) {
			mIns[i].close();
			mIns[i] = null;
			mLineSplits[i] = null;
			return;
		}
		
		mLineSplits[i] = StringUtil.trimmedSplit(line, ",");
	}

	public int getLength() {
		return MathUtil.nextPower(mHoursPerFile * mInPaths.length, 2);
	}

	public void close() throws IOException {
		for (int i = 0; i < mInPaths.length; i++) {
			if (mIns[i] != null) {
				mIns[i].close();
			}
		}
	}

	public double[] readSeries() throws IOException {
		if (mCursor >= mAllIPs.size()) {
			mCursorIP = null;
			return null;
		} else {
			mCursorIP = mAllIPs.get(mCursor);
		}
		
		Arrays.fill(cSeries, 0.0);
		for (int i = 0; i < mLineSplits.length; i++) {
			if (mLineSplits[i] != null && mCursorIP.equals(mLineSplits[i][0])) {
				for (int j = 1; j < mLineSplits[i].length; j++) {
					String[] vs = StringUtil.trimmedSplit(mLineSplits[i][j], ":");
					int index = mHoursPerFile * i + Integer.parseInt(vs[0]);
					long v = Long.parseLong(vs[1]);
					cSeries[index] = v;
				}
				
				readLine(i);
			}
		}
		
//		System.out.println(mCursorIP);
//		System.out.println(Arrays.toString(cSeries));
		mCursor++;
		
		copyPasteToNextPower(cSeries, mHoursPerFile * mInPaths.length);
		return cSeries;
	}

	private static void copyPasteToNextPower(double[] series, int begin) {
		int src = 0;
		for (int i = begin; i < series.length; i++, src++) {
			series[i] = series[src];
		}
	}

	public String getKey() {
		return mCursorIP;
	}

}
