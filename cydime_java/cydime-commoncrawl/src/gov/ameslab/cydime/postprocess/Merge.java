package gov.ameslab.cydime.postprocess;

import gov.ameslab.cydime.util.CUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class Merge {
	
	private static Map<String, String> mSiteMap;
	private static Map<String, Record> mSiteRecords;
	
	static class Record {

		private double mCount = 0.0;
		private double mSum = 0.0;
		private double mSumSq = 0.0;
		private double mMax = 0.0;
		
		public double getAvg() {
			return mSum / mCount;
		}
		
		public double getVar() {
			double avg = getAvg();
			return mSumSq / mCount - avg * avg;
		}
		
		public void add(double count, double avg, double var, double max) {
			mCount += count;
			mSum += avg * count;
			mSumSq += (var + avg * avg) * count;
			mMax = Math.max(mMax, max);
		}
		
		@Override
		public String toString() {
			return new StringBuilder()
				.append(mCount)
				.append(",")
				.append(getAvg())
				.append(",")
				.append(getVar())
				.append(",")
				.append(mMax)
				.toString();
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		init();
		for (String key : mSiteMap.keySet()) {
			System.out.println("Processing " + mSiteMap.get(key));
			
			mSiteRecords = CUtil.makeMap();
			for (File f : FileUtils.listFiles(new File(args[0]), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
				System.out.println("Reading " + f);
				read(f, key);
			}
			
			System.out.println("Writing");
			write(key);
		}
	}
	
	private static void write(String key) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(mSiteMap.get(key)));
		
		List<String> hostnames = CUtil.makeList(mSiteRecords.keySet());
		Collections.sort(hostnames);
		for (String hostname : hostnames) {
			out.write(hostname);
			out.write(",");
			out.write(mSiteRecords.get(hostname).toString());
			out.newLine();
		}			
		
		out.close();
	}
	
	private static void read(File f, String key) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line;
		while ((line = in.readLine()) != null) {
			String[] split = line.split("\\t");
			String site = split[0].substring(0, 1);
			if (!key.equals(site)) continue;
			
			double count = Double.parseDouble(split[1]);
			double avg = Double.parseDouble(split[2]);
			double var = Double.parseDouble(split[3]);
			double max = Double.parseDouble(split[4]);
			
			String hostname = split[0].substring(1);
			Record record = mSiteRecords.get(hostname);
			if (record == null) {
				record = new Record();
				mSiteRecords.put(hostname, record); 
			}
			record.add(count, avg, var, max);
		}
		in.close();		
	}

	private static void init() {
		mSiteMap = CUtil.makeMap();
		mSiteMap.put("M", "www.ameslab.gov");
		mSiteMap.put("A", "www.anl.gov");
		mSiteMap.put("B", "www.bnl.gov");
		mSiteMap.put("N", "www.nrel.gov");
		mSiteMap.put("O", "www.ornl.gov");
		mSiteMap.put("P", "www.pnl.gov");
	}
	
}
