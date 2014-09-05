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

import gov.ameslab.cydime.filter.NormalizeLog;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.StringToNominal;

public class WekaPreprocess {

	public static final String IP_SUFFIX = ".ip";
	public static final String ALL_SUFFIX = ".arff";
	public static final String TRAIN_SUFFIX = ".train.arff";
	public static final String TEST_SUFFIX = ".test.arff";
	public static final String REPORT_SUFFIX = ".report.arff";
	public static final String CSV_SUFFIX = ".csv";
	public static final String CSV_REPORT_SUFFIX = ".report.csv";
	
	public enum UnsupervisedFilter {
		StringToNominal,
		ReplaceMissingValues,
		Normalize,
		NormalizeLog,
		PrincipalComponents,
	}
//weka.filters.supervised.attribute.AttributeSelection -E "weka.attributeSelection.PrincipalComponents -R 0.95 -A 5" -S "weka.attributeSelection.Ranker -T -1.7976931348623157E308 -N 5"
	public enum SupervisedFilter {
		Discretize,
	}

	private static Filter createFilter(UnsupervisedFilter filter) {
		switch (filter) {
			case StringToNominal:
				StringToNominal f = new StringToNominal();
				f.setAttributeRange("first-last");
				return f;
				
			case ReplaceMissingValues:
				return new ReplaceMissingValues();
			
			case Normalize:
				Normalize n = new Normalize();
				n.setIgnoreClass(true);
				return n;
				
			case NormalizeLog:
				NormalizeLog g = new NormalizeLog();
				g.setIgnoreClass(true);
				return g;
				
			case PrincipalComponents:
				AttributeSelection a = new AttributeSelection();
//				a.setEvaluator(new PrincipalComponents());
		}
		
		return null;
	}
	
	private static Filter createFilter(SupervisedFilter filter) {
		switch (filter) {
			case Discretize:
				return new Discretize();
		}
		
		return null;
	}

	public static void filterUnsuperivsed(Instances all, String file, UnsupervisedFilter... filters) {
		for (int i = 0; i < filters.length; i++) {
			Filter f = createFilter(filters[i]);
//			System.out.print(f.getClass().getSimpleName() + "... ");
			try {
				f.setInputFormat(all);
				all = Filter.useFilter(all, f);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		save(all, file);
	}
	
	public static void filterUnsuperivsed(String file, UnsupervisedFilter... filters) throws IOException {
		Instances allInst = loadARFF(file);
		filterUnsuperivsed(allInst, file, filters);
	}
	
	public static void filterSuperivsed(Instances train, Instances test, String file, SupervisedFilter... filters) {
		for (int i = 0; i < filters.length; i++) {
			Filter f = createFilter(filters[i]);
//			System.out.print(f.getClass().getSimpleName() + "... ");
			try {
				f.setInputFormat(train);
				train = Filter.useFilter(train, f);
				test = Filter.useFilter(test, f);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		save(train, file + TRAIN_SUFFIX);
		save(test, file + TEST_SUFFIX);
	}
	
	public static void filterSuperivsed(String file, SupervisedFilter... filters) throws IOException {
		Instances trainInst = loadARFF(file + TRAIN_SUFFIX);
		Instances testInst = loadARFF(file + TEST_SUFFIX);
		filterSuperivsed(trainInst, testInst, file, filters);
	}
	
	public static void save(Instances data, String filename) {
		try {
			ArffSaver saver = new ArffSaver();
			saver.setInstances(data);
			saver.setFile(new File(filename));
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Instances loadARFF(String file) throws IOException {
		DataSource source;
		Instances allInst;
		try {
			source = new DataSource(file);
			allInst = source.getDataSet();
			allInst.setClassIndex(allInst.numAttributes() - 1);
		} catch (Exception ex) {
			throw new IOException(ex);
		}
		
		return allInst;
	}

	//Last inFile is used to get class label
	public static void mergeARFF(String outBase, String suffix, String ... inFile) throws IOException {
		mergeARFF(outBase, suffix, Arrays.asList(inFile));
	}

	//Last inFile is used to get class label
	public static void mergeARFF(String outBase, String suffix, List<String> inFile) throws IOException {
		String[] files = new String[inFile.size()];
		for (int i = 0; i < files.length; i++) {
			files[i] = inFile.get(i) + suffix;
		}
		mergeARFFFile(outBase + suffix, files);
	}

	//Last inFile is used to get class label
	public static void mergeARFFFile(String outFile, String ... inFile) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("@relation " + outFile);
		out.newLine();
		out.newLine();
		
		int[] attSize = new int[inFile.length];
		BufferedReader[] in = new BufferedReader[inFile.length];
		for (int i = 0; i < in.length; i++) {
			in[i] = new BufferedReader(new FileReader(inFile[i]));
		}
		
		for (int i = 0; i < in.length; i++) {
			String line = null;
			String trimmedLine = null;
			while (true) {
				line = in[i].readLine();
				trimmedLine = line.toLowerCase().trim();
				if (trimmedLine.startsWith("@data")) {
					break;
				} else if (trimmedLine.startsWith("@attribute class")) {
					continue;
				} else if (trimmedLine.startsWith("@attribute")) {
					out.write(line);
					out.newLine();
					attSize[i]++;
				}
			}
		}
		
		out.write("@attribute class numeric");
		out.newLine();
		out.newLine();
		out.write("@data");
		out.newLine();
		
		String line = "";
		while (line != null) {
			for (int i = 0; i < in.length; i++) {
				line = in[i].readLine();
				if (line == null || line.trim().isEmpty()) break;
				
				if (i == in.length - 1) {
					out.write(line);
				} else {
					String[] split = StringUtil.trimmedSplit(line, ",");
					for (int a = 0; a < attSize[i]; a++) {
						out.write(split[a]);
						out.write(",");
					}
				}
			}
			
			out.newLine();
		}
		
		out.close();
	}

	public static void concatARFF(String outBase, String suffix, String ... inFile) throws IOException {
		concatARFF(outBase, suffix, Arrays.asList(inFile));
	}

	public static void concatARFF(String outBase, String suffix, List<String> inFile) throws IOException {
		String[] files = new String[inFile.size()];
		for (int i = 0; i < files.length; i++) {
			files[i] = inFile.get(i) + suffix;
		}
		concatARFFFile(outBase + suffix, files);
	}

	private static class Attr {

		private String mLine;

		private String mNominalPrefix;
		private Set<String> mNominalSet;
		
		public Attr(String line) {
			mLine = line;
			
			int brace = line.indexOf("{");
			if (brace >= 0) {
				mNominalPrefix = line.substring(0, brace);
				String members = line.substring(brace + 1, line.length() - 1);
				String[] memberArray = StringUtil.trimmedSplit(members, ",");
				mNominalSet = CUtil.asSet(memberArray);
			}
		}

		public void testAndMerge(Attr a) {
			if (isNominal()) {
				if (a.isNominal()) {
					if (mNominalPrefix.equals(a.mNominalPrefix)) {
						mNominalSet.addAll(a.mNominalSet);
					} else {
						throw new IllegalArgumentException("Error: " + mLine + " does not match " + a.mLine);
					}
				} else {
					throw new IllegalArgumentException("Error: " + mLine + " does not match " + a.mLine);
				}
			} else {
				if (a.isNominal() || !mLine.equals(a.mLine)) {
					throw new IllegalArgumentException("Error: " + mLine + " does not match " + a.mLine);
				}
			}
		}
		
		private boolean isNominal() {
			return mNominalPrefix != null;
		}
		
		@Override
		public String toString() {
			if (isNominal()) {
				StringBuilder b = new StringBuilder();
				b.append(mNominalPrefix);
				b.append("{");
				for (String m : mNominalSet) {
					b.append(m);
					b.append(",");
				}
				b.deleteCharAt(b.length() - 1);
				b.append("}");
				return b.toString();
			} else {
				return mLine;
			}
		}
		
	}
	
	public static void concatARFFFile(String outFile, String ... inFile) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("@relation " + outFile);
		out.newLine();
		out.newLine();
		
		BufferedReader[] in = new BufferedReader[inFile.length];
		for (int i = 0; i < in.length; i++) {
			in[i] = new BufferedReader(new FileReader(inFile[i]));
		}
		
		List<Attr> atts = CUtil.makeList();
		for (int i = 0; i < in.length; i++) {
			String line = null;
			String trimmedLine = null;
			int attIndex = 0;
			while (true) {
				line = in[i].readLine();
				trimmedLine = line.toLowerCase().trim();
				if (trimmedLine.startsWith("@data")) {
					break;
				} else if (trimmedLine.startsWith("@attribute class")) {
					continue;
				} else if (trimmedLine.startsWith("@attribute")) {
					Attr a = new Attr(line);
					if (i == 0) {
						atts.add(a);
					} else if (attIndex >= atts.size()) {
						out.close();
						throw new IllegalArgumentException("Error: Unmatched attribute " + line + " in " + inFile[i]);
					} else {
						atts.get(attIndex).testAndMerge(a);
					}
					
					attIndex++;
				}
			}
			
			if (attIndex != atts.size()) {
				out.close();
				throw new IllegalArgumentException("Error: Expected attribute size is " + atts.size() + ", but only found " + attIndex + " in " + inFile[i]);
			}
		}
		
		for (Attr a : atts) {
			out.write(a.toString());
			out.newLine();
		}
		
		out.write("@attribute class numeric");
		out.newLine();
		out.newLine();
		out.write("@data");
		out.newLine();
		
		for (int i = 0; i < in.length; i++) {
			String line = null;
			while ((line = in[i].readLine()) != null) {
				if (line.trim().isEmpty()) break;
				out.write(line);
				out.newLine();
			}
		}
		
		out.close();
	}

}