package gov.ameslab.cydime.preprocess.dailyprofile;

import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.FeatureSet;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.preprocess.service.ServiceParser;
import gov.ameslab.cydime.util.ARFFWriter;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.FileUtil;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.MathUtil;
import gov.ameslab.cydime.util.Percentile;
import gov.ameslab.cydime.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [Time-of-day profile]
 * We break a day into six 4-hourly bins, and aggregate the volume for each bin for each external resource.
 * The six bins are normalized so that they sum to 1.
 * 
 * [Service profile]
 * For each external resource, we create a set of associated services and collect statistics
 * for each service in the set. We map traffic to service type using a table of common protocol/port combinations,
 * and if a TCP or UDP connection is observed on an unrecognized high port or a protocol other than TCP/UDP
 * is observed, it is listed as OTHER. To adjust for magnitude difference among different services, we normalize
 * each volume to its percentile when ordered in the entire data set. As an extra categorical feature we also
 * select the most-used service for each external resource determined by byte volume.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class DailyProfile extends FeatureSet {

	private static final Logger Log = Logger.getLogger(DailyProfile.class.getName());
	
	public enum Normalizer { SERVICE_SUM, TIME_SUM, RAW_SERVICE_NORM, RAW_TIME_NORM }
	
	private String mDPOutPath;
	private Map<String, double[][]> mIDProfile;
	private Calendar mCal = GregorianCalendar.getInstance();
	private IndexedList<Integer> mHourIndex;
	private IndexedList<String> mServiceIndex;
	
	public DailyProfile(List<String> ids, String inPath, String outPath) {
		super(ids, inPath, outPath);
	}

	public InstanceDatabase run(Normalizer type) throws IOException {
		read();
		switch (type) {
			case SERVICE_SUM:
				mDPOutPath = mCurrentOutPath + ".service_sum";
				prepareInstancesServiceSum();
				break;
			case TIME_SUM:
				mDPOutPath = mCurrentOutPath + ".time_sum";
				prepareInstancesTimeSum();
				break;
			case RAW_SERVICE_NORM:
				mDPOutPath = mCurrentOutPath + ".service";
				prepareInstancesServiceNorm();
				break;
			case RAW_TIME_NORM:
				mDPOutPath = mCurrentOutPath + ".time";
				prepareInstancesTimeNorm();
				break;
		}
		
		FileUtil.copy(mDPOutPath + WekaPreprocess.ALL_SUFFIX, mDPOutPath + WekaPreprocess.REPORT_SUFFIX);
		return new InstanceDatabase(mDPOutPath, mIDs);
	}
	
	private void read() throws IOException {
		Log.log(Level.INFO, "Processing daily profile...");
		
		Set<Integer> hours = CUtil.makeSet();
		BufferedReader in = new BufferedReader(new FileReader(mCurrentInPath));
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			long epoch = Long.parseLong(split[4]) * 1000;
			int hour = getHourOfDay(new Date(epoch));
			hours.add(hour);
		}
		in.close();
		
		List<Integer> hourList = CUtil.makeList(hours);
		Collections.sort(hourList);
		
		mIDProfile = CUtil.makeMap();
		mHourIndex = new IndexedList<Integer>(hourList);
		mServiceIndex = new IndexedList<String>(ServiceParser.SERVICES);
		
		in = new BufferedReader(new FileReader(mCurrentInPath));
		line = in.readLine();
		while ((line = in.readLine()) != null) {
			String[] split = StringUtil.trimmedSplit(line, ",");
			String id = split[1];
			double weight = Double.parseDouble(split[7]);
			long epoch = Long.parseLong(split[4]) * 1000;
			int hour = getHourOfDay(new Date(epoch));
			int hourIndex = mHourIndex.getIndex(hour);
			String src = split[2];
			String dest = split[3];
			for (String serv : ServiceParser.parse(src, dest)) {
				int servIndex = mServiceIndex.getIndex(serv);
				add(id, servIndex, hourIndex, weight);
			}
		}
		in.close();
	}
	
	private void add(String id, int servIndex, int hourIndex, double weight) {
		double[][] profile = mIDProfile.get(id);
		if (profile == null) {
			profile = new double[mServiceIndex.size()][mHourIndex.size()];
			mIDProfile.put(id, profile);
		}
		
		profile[servIndex][hourIndex] += weight;
	}

	private int getHourOfDay(Date date) {
		mCal.setTime(date);
		return mCal.get(Calendar.HOUR_OF_DAY);
	}
	
	private void prepareInstancesServiceSum() throws IOException {
		Log.log(Level.INFO, "Normalizing...");
		
		for (double[][] profile : mIDProfile.values()) {
			for (int i = 0; i < profile.length; i++) {
				profile[i][0] = MathUtil.sum(profile[i]);
			}
		}
		
		for (int i = 0; i < ServiceParser.SERVICES.length; i++) {
			Percentile p = new Percentile();
			
			for (double[][] profile : mIDProfile.values()) {
				p.add(profile[i][0]);
			}
			
			p.compute();
			
			for (double[][] profile : mIDProfile.values()) {
				profile[i][0] = p.getPercentile(profile[i][0]);
			}
		}
		
		Log.log(Level.INFO, "Writing...");
		
		List<String> atts = CUtil.makeList();
		for (int i = 0; i < ServiceParser.SERVICES.length; i++) {
			atts.add("service_" + ServiceParser.SERVICES[i] + " numeric");
		}		
		ARFFWriter out = new ARFFWriter(mDPOutPath + WekaPreprocess.ALL_SUFFIX, "services", ARFFWriter.CLASS_BINARY, atts); 
		
		String[] values = new String[ServiceParser.SERVICES.length + 1];
		values[values.length - 1] = "?";
		for (String id : mIDs) {			
			double[][] profile = mIDProfile.get(id);
			if (profile == null) {
				profile = new double[mServiceIndex.size()][mHourIndex.size()];
			}
			
			for (int i = 0; i < profile.length; i++) {
				values[i] = String.valueOf(profile[i][0]);
			}			
			out.writeValues(values);
		}
		
		out.close();
	}
	
	private void prepareInstancesTimeSum() throws IOException {
		Log.log(Level.INFO, "Normalizing...");
		
		for (double[][] profile : mIDProfile.values()) {
			for (int i = 1; i < profile.length; i++) {
				for (int j = 0; j < profile[0].length; j++) {
					profile[0][j] += profile[i][j];
				}
			}
			
			MathUtil.normalize(profile[0]);
		}
		
		Log.log(Level.INFO, "Writing...");
		
		List<String> atts = CUtil.makeList();
		for (int i = 0; i < mHourIndex.size(); i++) {
			atts.add("hour_" + (i * 4) + " numeric");
		}		
		ARFFWriter out = new ARFFWriter(mDPOutPath + WekaPreprocess.ALL_SUFFIX, "time_of_day", ARFFWriter.CLASS_BINARY, atts); 
		
		String[] values = new String[mHourIndex.size() + 1];
		values[values.length - 1] = "?";
		for (String id : mIDs) {			
			double[][] profile = mIDProfile.get(id);
			if (profile == null) {
				profile = new double[mServiceIndex.size()][mHourIndex.size()];
			}
			
			for (int i = 0; i < profile[0].length; i++) {
				values[i] = String.valueOf(profile[0][i]);
			}			
			out.writeValues(values);
		}
		
		out.close();
	}
	
	private void prepareInstancesServiceNorm() throws IOException {
		Log.log(Level.INFO, "Normalizing...");
		
		for (int i = 0; i < ServiceParser.SERVICES.length; i++) {
			Percentile p = new Percentile();
			
			for (double[][] profile : mIDProfile.values()) {
				for (int j = 0; j < profile[i].length; j++) {
					p.add(profile[i][j]);
				}
			}
			
			p.compute();
			
			for (double[][] profile : mIDProfile.values()) {
				for (int j = 0; j < profile[i].length; j++) {
					profile[i][j] = p.getPercentile(profile[i][j]);
				}
			}
		}
		
		Log.log(Level.INFO, "Writing...");
		
		List<String> atts = CUtil.makeList();
		for (int i = 0; i < ServiceParser.SERVICES.length; i++) {
			for (int j = 0; j < mHourIndex.size(); j++) {
				atts.add("service_" + ServiceParser.SERVICES[i] + "_hour_" + (j * 4) + " numeric");
			}
		}
		ARFFWriter out = new ARFFWriter(mDPOutPath + WekaPreprocess.ALL_SUFFIX, "service-norm_time", ARFFWriter.CLASS_BINARY, atts); 
		
		String[] values = new String[ServiceParser.SERVICES.length * mHourIndex.size() + 1];
		values[values.length - 1] = "?";
		for (String id : mIDs) {			
			double[][] profile = mIDProfile.get(id);
			if (profile == null) {
				profile = new double[mServiceIndex.size()][mHourIndex.size()];
			}
			
			int v = 0;
			for (int i = 0; i < profile.length; i++) {
				for (int j = 0; j < profile[i].length; j++) {
					values[v++] = String.valueOf(profile[i][j]);
				}
			}			
			out.writeValues(values);
		}
		
		out.close();
	}

	private void prepareInstancesTimeNorm() throws IOException {
		Log.log(Level.INFO, "Normalizing...");
		
		for (double[][] profile : mIDProfile.values()) {
			for (int i = 0; i < profile.length; i++) {
				MathUtil.normalize(profile[i]);
			}
		}
		
		Log.log(Level.INFO, "Writing...");
		
		List<String> atts = CUtil.makeList();
		for (int i = 0; i < ServiceParser.SERVICES.length; i++) {
			for (int j = 0; j < mHourIndex.size(); j++) {
				atts.add("service_" + ServiceParser.SERVICES[i] + "_hour_" + (j * 4) + " numeric");
			}
		}
		ARFFWriter out = new ARFFWriter(mDPOutPath + WekaPreprocess.ALL_SUFFIX, "service_time-norm", ARFFWriter.CLASS_BINARY, atts); 
		
		String[] values = new String[ServiceParser.SERVICES.length * mHourIndex.size() + 1];
		values[values.length - 1] = "?";
		for (String id : mIDs) {			
			double[][] profile = mIDProfile.get(id);
			if (profile == null) {
				profile = new double[mServiceIndex.size()][mHourIndex.size()];
			}
			
			int v = 0;
			for (int i = 0; i < profile.length; i++) {
				for (int j = 0; j < profile[i].length; j++) {
					values[v++] = String.valueOf(profile[i][j]);
				}
			}			
			out.writeValues(values);
		}
		
		out.close();
	}
	
}
