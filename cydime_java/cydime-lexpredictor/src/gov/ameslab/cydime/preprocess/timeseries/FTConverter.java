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

import gov.ameslab.cydime.util.MathUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Fourier transform using Apache Math's FastFourierTransformer.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class FTConverter implements Converter {

	private static final String NAME = "Fourier";
	private static final int[] CYCLE_PERIODS = new int[] {24, 24*7};
	private static final DecimalFormat FORMAT = new DecimalFormat("0.000");

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getSize() {
		return CYCLE_PERIODS.length;
	}

	@Override
	public void reset() {
	}

	@Override
	public void learn(String[] inPaths, String inFile, String outFile) throws IOException {
	}

	@Override
	public void convert(String[] inPaths, String inFile, String outFile) throws IOException {
		FastFourierTransformer t = new FastFourierTransformer(DftNormalization.STANDARD);
		SeriesReader in = new SeriesReader(inPaths, inFile, 48);
		
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile + "." + NAME));
		
		int LENGTH = in.getLength();
		int[] frequencyIndex = getFrequencies(LENGTH);
		
		while (true) {
			double[] time = in.readSeries();
			if (time == null) break;
			
			double max = MathUtil.max(time);
			MathUtil.divide(time, max);
			
			Complex[] freq = t.transform(time, TransformType.FORWARD);
			out.write(in.getKey());
			for (int i = 0; i < frequencyIndex.length; i++) {
				out.write(",");
				out.write(FORMAT.format(freq[frequencyIndex[i]].abs()));
			}
			out.newLine();
		}
		in.close();
		out.close();
	}

	private int[] getFrequencies(int nextPower) {
		int[] freq = new int[CYCLE_PERIODS.length];
		for (int i = 0; i < freq.length; i++) {
			freq[i] = (int) Math.round(1.0 * nextPower / CYCLE_PERIODS[i]);
		}
		return freq;
	}

}
