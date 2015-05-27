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

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sorts the time series and service feature files so that records with the same IP
 * are grouped together (and also in increasing timestamps).
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CydimeSort {

	private static final Logger Log = Logger.getLogger(CydimeSort.class.getName());
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			printUsage();
			return;
		}
		
		new CydimeSort(args[0]).run();
	}

	private static void printUsage() {
		System.out.println("[java] CydimeSort FEATURE_DIR");
		System.out.println("    FEATURE_DIR: date path specifying feature files");
	}
	
	public CydimeSort(String featurePath) {
		Config.INSTANCE.setParam(featurePath);
	}
	
	private void run() throws IOException {
		for (String featureSet : new String[] {Config.FEATURE_IP_DIR, Config.FEATURE_ASN_DIR, Config.FEATURE_INT_DIR}) {
			Config.INSTANCE.setFeatureSet(featureSet);
			sortFile(Config.INSTANCE.getCurrentFeaturePath() + Config.INSTANCE.getTimeSeries());
			sortFile(Config.INSTANCE.getCurrentFeaturePath() + Config.INSTANCE.getService());
		}
	}

	private void sortFile(String file) throws IOException {
		if (!new File(file).exists()) {
			Log.log(Level.INFO, "File not found, skipping: {0}", file);
			return;
		}
		
		Log.log(Level.INFO, "Sorting {0} ...", file);
		
		List<String> lines = CUtil.makeList();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String header = in.readLine();
		String line;
		while ((line = in.readLine()) != null) {
			lines.add(line);
		}
		in.close();
		
		Collections.sort(lines);
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write(header);
		out.newLine();
		for (String a : lines) {
			out.write(a);
			out.newLine();
		}
		out.close();
	}

}
