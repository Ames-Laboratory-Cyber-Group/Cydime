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

package gov.ameslab.cydime.model;

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.Normalize;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.IndexedList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import weka.core.Instance;
import weka.core.Instances;

/**
 * A wrapper for Weka's Instances that maintains the IP (as key) for each Instance.
 * Also manages "report" Instances where the values are usually in their raw form (before normalization) for displaying.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class InstanceDatabase {

	private static final Logger Log = Logger.getLogger(InstanceDatabase.class.getName());
	
	private static final String TRAIN_PREFIX = "train:";
	
	private String mPath;
	private List<String> mIDs;

	private Instances mInstances;
	private Map<String, Instance> mInstanceMap;
	private Instances mReportInstances;
	private Map<String, Instance> mReportInstanceMap;
	private IndexedList<String> mAttributeIndex;
	private IndexedList<String> mReportAttributeIndex;
	
	private List<String> mIDTrain;
	private List<String> mIDTest;
	private Instances mTrainInstances;
	private Instances mTestInstances;
	
	public InstanceDatabase(String path) {
		this(path, CUtil.<String>makeList());
	}

	public InstanceDatabase(String path, List<String> ids) {
		mPath = path;
		mIDs = ids;
	}

	public List<String> getIDs() {
		return mIDs;
	}

	public String getARFFPath() {
		return mPath + WekaPreprocess.ALL_SUFFIX;
	}
	
	public String getReportARFFPath() {
		return mPath + WekaPreprocess.REPORT_SUFFIX;
	}
	
	public String getCSVPath() {
		return mPath + WekaPreprocess.CSV_SUFFIX;
	}
	
	public String getReportCSVPath() {
		return mPath + WekaPreprocess.CSV_REPORT_SUFFIX;
	}
	
	public String getIDPath() {
		return mPath + WekaPreprocess.ID_SUFFIX;
	}

	public String getTrainPath() {
		return mPath + WekaPreprocess.TRAIN_SUFFIX;
	}
	
	public String getTestPath() {
		return mPath + WekaPreprocess.TEST_SUFFIX;
	}
		
	public void normalize() throws IOException {
		WekaPreprocess.filterUnsuperivsed(getARFFPath(),
				Normalize);
		
		WekaPreprocess.filterUnsuperivsed(getReportARFFPath(),
				Normalize);
	}

	public void saveIPs() throws IOException {
		FileUtil.writeFile(getIDPath(), mIDs);
	}

	private void loadInstances() {
		if (mInstances != null) return;
		
		try {
			mInstances = WekaPreprocess.loadARFF(getARFFPath());
			mReportInstances = WekaPreprocess.loadARFF(getReportARFFPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		mInstanceMap = CUtil.makeMap();
		for (int i = 0; i < mInstances.numInstances(); i++) {
			mInstanceMap.put(mIDs.get(i), mInstances.instance(i));
		}
		
		mReportInstanceMap = CUtil.makeMap();
		for (int i = 0; i < mReportInstances.numInstances(); i++) {
			mReportInstanceMap.put(mIDs.get(i), mReportInstances.instance(i));
		}
		
		List<String> names = CUtil.makeList();
		for (int i = 0; i < mInstances.numAttributes(); i++) {
			names.add(mInstances.attribute(i).name());
		}
		mAttributeIndex = new IndexedList<String>(names);
		
		names = CUtil.makeList();
		for (int i = 0; i < mReportInstances.numAttributes(); i++) {
			names.add(mReportInstances.attribute(i).name());
		}
		mReportAttributeIndex = new IndexedList<String>(names);
	}
	
	//Requires loadInstances
	public Instances getWekaInstances() {
		loadInstances();
		return mInstances;
	}

	//Requires loadInstances
	public Instance getWekaInstance(String id) {
		loadInstances();
		return mInstanceMap.get(id);
	}

	//Requires loadInstances
	public Instances getWekaReportInstances() {
		loadInstances();
		return mReportInstances;
	}

	//Requires loadInstances
	public Instance getWekaReportInstance(String id) {
		loadInstances();
		return mReportInstanceMap.get(id);
	}

	//Requires loadInstances
	public boolean hasLabel(String id) {
		loadInstances();
		Instance inst = getWekaInstance(id);
		return !inst.classIsMissing();
	}
	
	//Requires loadInstances
	public double getLabel(String id) {
		loadInstances();
		Instance inst = getWekaInstance(id);
		if (inst.classIsMissing()) {
			return Double.NaN;
		} else {
			return inst.classValue();
		}
	}
	
	//Requires loadInstances
	public void setLabel(String id, Double label) {
		loadInstances();
		Instance inst = getWekaInstance(id);
		if (label == null || label.isNaN()) {
			inst.setClassMissing();
		} else {
			inst.setClassValue(label);
		}
		
		Instance instReport = getWekaReportInstance(id);
		if (label == null || label.isNaN()) {
			instReport.setClassMissing();
		} else {
			instReport.setClassValue(label);
		}
	}

	//Requires loadInstances
	public int getAttributeIndex(String name) {
		loadInstances();
		return mAttributeIndex.getIndex(name);
	}
	
	//Requires loadInstances
	public List<String> getAttributeList() {
		loadInstances();
		return mAttributeIndex.getList();
	}
	
	//Requires loadInstances
	public int removeAttribute(String name) {
		loadInstances();
		int i = mAttributeIndex.remove(name);
		mInstances.deleteAttributeAt(i);
		return i;
	}

	//Requires loadInstances
	private void loadTrainTest() {
		if (mTrainInstances != null) return;
		loadInstances();
		
		mTrainInstances = new Instances(mInstances, 0);
		mTestInstances = new Instances(mInstances, 0);
		mIDTrain = CUtil.makeList();
		mIDTest = CUtil.makeList();
		
		for (String id : mIDs) {
			Instance inst = mInstanceMap.get(id);
			
			if (inst.classIsMissing()) {
				inst.setDataset(mTestInstances);
				mTestInstances.add(inst);
				mIDTest.add(id);
			} else {
				inst.setDataset(mTrainInstances);
				mTrainInstances.add(inst);
				mIDTrain.add(id);
			}
		}
	}
	
	//Requires loadInstances
	public void write() {
		loadInstances();
		WekaPreprocess.save(mInstances, getARFFPath());
		WekaPreprocess.save(mReportInstances, getReportARFFPath());
	}

	public void writeReport() throws IOException {
		loadInstances();
		writeReport(getCSVPath(), mInstanceMap, mAttributeIndex);
		writeReport(getReportCSVPath(), mReportInstanceMap, mReportAttributeIndex);
	}

	private void writeReport(String file, Map<String, Instance> instanceMap, IndexedList<String> attributeIndex) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		out.write("ID");
		for (String att : attributeIndex.getList()) {
			out.write(",");
			out.write(att);
		}
		out.newLine();
		
		for (String id : mIDs) {
			Instance inst = instanceMap.get(id);
			
			out.write(id + "," + inst);
			out.newLine();
		}
		
		out.close();
	}

	//Requires loadTrainTest
	public List<String> getTrainIDs() {
		loadTrainTest();
		return mIDTrain;
	}

	//Requires loadTrainTest
	public List<String> getTestIDs() {
		loadTrainTest();
		return mIDTest;
	}

	//Requires loadTrainTest
	public Instances getWekaTrain() {
		loadTrainTest();
		return mTrainInstances;
	}

	//Requires loadTrainTest
	public Instances getWekaTest() {
		loadTrainTest();
		return mTestInstances;
	}

	public static boolean exists(String path) throws IOException {
		InstanceDatabase insts = new InstanceDatabase(path);
		File id = new File(insts.getIDPath());
		File arff = new File(insts.getARFFPath());
		File report = new File(insts.getReportARFFPath());

		return id.exists() && arff.exists() && report.exists(); 
	}
	
	public static InstanceDatabase load(String path) throws IOException {
		InstanceDatabase insts = new InstanceDatabase(path);
		File id = new File(insts.getIDPath());
		File arff = new File(insts.getARFFPath());
		File report = new File(insts.getReportARFFPath());
		
		if (!id.exists()) throw new IOException("Error: " + insts.getIDPath() + " not found.");
		if (!arff.exists()) throw new IOException("Error: " + insts.getARFFPath() + " not found.");
		if (!report.exists()) throw new IOException("Error: " + insts.getReportARFFPath() + " not found.");
		
		insts.mIDs = FileUtil.readFile(insts.getIDPath());		
		return insts;
	}

	public static InstanceDatabase mergeFeatures(String path, InstanceDatabase ... insts) throws IOException {
		for (int i = 1; i < insts.length; i++) {
			if (!insts[0].mIDs.equals(insts[i].mIDs)) {
				throw new IllegalArgumentException("Error: ID list of " + insts[0].mPath + " and " + insts[i].mPath + " do not match.");
			}
		}
		
		String[] paths = getPaths(insts);
		WekaPreprocess.mergeARFF(path, WekaPreprocess.ALL_SUFFIX, paths);		
		WekaPreprocess.mergeARFF(path, WekaPreprocess.REPORT_SUFFIX, paths);		
		
		return new InstanceDatabase(path, CUtil.makeList(insts[0].mIDs));
	}
	
	public static InstanceDatabase mergeInstances(String path, InstanceDatabase ... insts) throws IOException {
		List<String> allIDs = checkAndConcatIPs(insts);
		
		String[] paths = getPaths(insts);
		WekaPreprocess.concatARFF(path, WekaPreprocess.ALL_SUFFIX, paths);		
		WekaPreprocess.concatARFF(path, WekaPreprocess.REPORT_SUFFIX, paths);		
		
		InstanceDatabase merged = new InstanceDatabase(path, allIDs);
		merged.saveIPs();
		return merged;
	}
	
	private static List<String> checkAndConcatIPs(InstanceDatabase[] insts) {
		List<String> allIDList = CUtil.makeList();
		Set<String> allIDs = CUtil.makeSet();
		for (int i = 0; i < insts.length; i++) {
			for (String id : insts[i].mIDs) {
				if (allIDs.contains(id)) {
					throw new IllegalArgumentException("Error: Duplicate ID found: " + id);
				}
			}
			allIDs.addAll(insts[i].mIDs);
			allIDList.addAll(insts[i].mIDs);
		}
		
		return allIDList;
	}

	private static String[] getPaths(InstanceDatabase[] insts) {
		String[] paths = new String[insts.length];
		for (int i = 0; i < paths.length; i++) {
			paths[i] = insts[i].mPath;
		}
		return paths;
	}

	public static String trainToID(String train) {
		if (train.startsWith(TRAIN_PREFIX)) {
			return train.substring(TRAIN_PREFIX.length());
		} else {
			return train;
		}
	}
	
	public static String idToTrain(String id) {
		if (id.startsWith(TRAIN_PREFIX)) {
			return id;
		} else {
			return TRAIN_PREFIX + id;
		}
	}

}