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

package gov.ameslab.cydime.explorer.models;

import gov.ameslab.cydime.model.DomainDatabase;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.BipartiteGraph;
import gov.ameslab.cydime.util.CSVReader;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.HistogramLong;
import gov.ameslab.cydime.util.Range;
import gov.ameslab.cydime.util.StringUtil;
import gov.ameslab.cydime.util.models.ServiceFormatter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * MVC Model for summary statistics table.
 * Holds nested maps for fast retrieval of time series data.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class StatData {

    private static final ServiceFormatter ServFormatter = new ServiceFormatter();
    private static final String NOT_AVAILABLE = "N/A";
    private static final String TIMESERIES_TOTAL = "TOTAL";
    private static final String TIMESERIES_OTHER = "OTHER";
	
	private DomainDatabase mDomainDB;
    private Range mExtRange;
    private Map<String, Object[]> mExtIPRecord;
    private Map<String, Map<String, HistogramLong<Long>>> mExtIPServTimeseriesByte;
    private Map<String, HistogramLong<Long>> mExtIPOtherTimeseriesByte;
    
    private Range mIntRange;
    private Map<String, Object[]> mIntIPRecord;
    private Map<String, Map<String, HistogramLong<Long>>> mIntIPServTimeseriesByte;
    private Map<String, HistogramLong<Long>> mIntIPOtherTimeseriesByte;
    
    private BipartiteGraph<String, String> mIntExtGraph;
    
    private List<String> mExtIPs;
    private Set<String> mExtIPSet;
    private List<String> mIntIPs;
    private Set<String> mIntIPSet;
	
	public Range getExtRange() {
		return mExtRange;
	}
	
	public Range getIntRange() {
		return mIntRange;
	}
	
	public Set<String> getExtIPNeighborsOf(List<String> intIPs) {
		Set<String> ips = CUtil.makeSet();
		for (String ip : intIPs) {
			ips.addAll(mIntExtGraph.getNeighborsOfA(ip));
		}
		return ips;
	}
	
	public Set<String> getIntIPNeighborsOf(List<String> extIPs) {
		Set<String> ips = CUtil.makeSet();
		for (String ip : extIPs) {
			ips.addAll(mIntExtGraph.getNeighborsOfB(ip));
		}
		return ips;
	}
	
	public List<Object[]> getExtIPRecords(Collection<String> extIPs) {
		List<Object[]> rec = CUtil.makeList();
		for (String ip : extIPs) {
			rec.add(mExtIPRecord.get(ip));
		}
		return rec;
	}
	
	public List<Object[]> getIntIPRecords(Collection<String> intIPs) {
		List<Object[]> rec = CUtil.makeList();
		for (String ip : intIPs) {
			rec.add(mIntIPRecord.get(ip));
		}
		return rec;
	}

	public Map<String, HistogramLong<Long>> getExtServTimeseriesByte(String ip) {
        return mExtIPServTimeseriesByte.get(ip);
    }
    
	public Map<String, HistogramLong<Long>> getIntServTimeseriesByte(String ip) {
        return mIntIPServTimeseriesByte.get(ip);
    }
    
	public void load(DomainDatabase domainDB, List<String> extIPs, List<String> intIPs) throws IOException {
		mDomainDB = domainDB;
		mExtIPs = extIPs;
		mExtIPSet = CUtil.makeSet(mExtIPs);
		mIntIPs = intIPs;
		mIntIPSet = CUtil.makeSet(mIntIPs);
		
        loadExtTable();
        loadIntTable();
        loadExtTimeseries();
        loadIntTimeseries();
        loadGraph();
    }
	
	private void loadExtTable() throws IOException {
		System.out.println("Reading features (external) ...");
		
        mExtIPRecord = CUtil.makeMap();
        
        CSVReader in = new CSVReader();
//        IP,service,cc,total_records,total_bytes,earliest_starttime,latest_endtime,total_peercount,total_localport,total_remoteport,ratio_local_remote_port,Bytes_Fourier0,Bytes_Fourier1,access_hours,access_days,workhour_perc,service_icmp,service_ssh,service_smtp,service_domain,service_rtsp,service_http,service_mail,service_vpn,service_OTHER,hour_0,hour_4,hour_8,hour_12,hour_16,hour_20,class
        in.add(Config.INSTANCE.getBasePath() + WekaPreprocess.CSV_REPORT_SUFFIX);
//        IP,service,cc,total_records,total_bytes,earliest_starttime,latest_endtime,total_peercount,total_localport,total_remoteport,ratio_local_remote_port,Bytes_Fourier0,Bytes_Fourier1,access_hours,access_days,workhour_perc,service_icmp,service_ssh,service_smtp,service_domain,service_rtsp,service_http,service_mail,service_vpn,service_OTHER,hour_0,hour_4,hour_8,hour_12,hour_16,hour_20,class
        in.add(Config.INSTANCE.getBaseNormPath() + WekaPreprocess.CSV_SUFFIX);
//        IP,hierarchy_stack,baseNorm_stack,class
        in.add(Config.INSTANCE.getAllPredictedPath() + WekaPreprocess.CSV_SUFFIX);
//      IP,score
        in.add(Config.INSTANCE.getFinalResultPath());
        int i = 0;
        while (in.readLine()) {
        	String ip = in.get("IP");
        	String label = in.get("class");
            String serv = in.get("service");
            String domain = mDomainDB.getDomain(ip);
            String whois = mDomainDB.getWhois(ip);
            String semantic = in.get(2, "hierarchy_stack");
            String strength = in.get(2, "baseNorm_stack");
            
            if (!mExtIPSet.contains(ip)) continue;
            
    		if (domain == null || domain.isEmpty()) {
            	domain = NOT_AVAILABLE;
            }
            
            if (whois == null || whois.isEmpty()) {
            	whois = NOT_AVAILABLE;
            }
            
            Object[] rec = new Object[] {
            		(label.equals("?")) ? null : Double.parseDouble(label),  //label
                    Double.parseDouble(in.get(3, "score")),
            		ip, //IP
            		(semantic == null) ? null : Double.parseDouble(semantic),
            		(strength == null) ? null : Double.parseDouble(strength),
            		serv, //service
            		domain, //domain
            		whois, //whois
            		in.get("cc"),						//cc
            		(long) Double.parseDouble(in.get("total_records")),		//total_records			raw 
            		(long) Double.parseDouble(in.get("total_bytes")),		//total_bytes			raw
            		(long) Double.parseDouble(in.get("total_peercount")),		//total_peercount		raw
            		(long) Double.parseDouble(in.get("total_localport")),		//total_localport		raw
            		(long) Double.parseDouble(in.get("total_remoteport")),		//total_remoteport			raw
            		Double.parseDouble(in.get(1, "Bytes_Fourier0")),	//Bytes_Fourier0		%
            		Double.parseDouble(in.get(1, "Bytes_Fourier1")),	//Bytes_Fourier1		%
            		(long) Double.parseDouble(in.get("access_hours")),		//access_hours			raw
            		(long) Double.parseDouble(in.get("access_days")),		//access_days			raw
            		Double.parseDouble(in.get("workhour_perc")),	//workhour_perc			%
            };
            
            mExtIPRecord.put(ip, rec);
            
            if (++i % 100000 == 0) {
                System.out.println((i / 1000) + "k IPs ...");
            }
        }
        in.close();
    }
    
	private void loadIntTable() throws IOException {
		System.out.println("Reading features (internal) ...");
		
        mIntIPRecord = CUtil.makeMap();
        
        CSVReader in = new CSVReader();
        
        in.add(Config.INSTANCE.getIntBasePath() + WekaPreprocess.CSV_REPORT_SUFFIX);
        
        in.add(Config.INSTANCE.getIntBaseNormPath() + WekaPreprocess.CSV_REPORT_SUFFIX);
        int i = 0;
        while (in.readLine()) {
            String ip = in.get("IP");
            
            if (!mIntIPSet.contains(ip)) continue;
            
            Object[] rec = new Object[] {
            		ip,
            		(long) Double.parseDouble(in.get("total_records")),
            		(long) Double.parseDouble(in.get("total_bytes")),
            		(long) Double.parseDouble(in.get("total_peercount")),
            		(long) Double.parseDouble(in.get("total_localport")),
            		(long) Double.parseDouble(in.get("total_remoteport")),
            		Double.parseDouble(in.get(1, "Bytes_Fourier0")),
            		Double.parseDouble(in.get(1, "Bytes_Fourier1")),
            		(long) Double.parseDouble(in.get("access_hours")),
            		(long) Double.parseDouble(in.get("access_days")),
            		Double.parseDouble(in.get("workhour_perc")),
            };
            
            mIntIPRecord.put(ip, rec);
            
            if (++i % 100000 == 0) {
                System.out.println((i / 1000) + "k IPs ...");
            }
        }
        in.close();
	}

    private static void updateTimeseries(Map<String, Map<String, HistogramLong<Long>>> ipServTimeseriesByte, String ip, String serv, long time, long bytes) {
        Map<String, HistogramLong<Long>> servTimeseriesByte = ipServTimeseriesByte.get(ip);
        if (servTimeseriesByte == null) {
            servTimeseriesByte = CUtil.makeMap();
            ipServTimeseriesByte.put(ip, servTimeseriesByte);
        }

        HistogramLong<Long> timeseriesByte = servTimeseriesByte.get(serv);
        if (timeseriesByte == null) {
            timeseriesByte = new HistogramLong<Long>();
            servTimeseriesByte.put(serv, timeseriesByte);
        }

        timeseriesByte.increment(time, bytes);
    }
    
    private static void mergeTimeseries(Map<String, HistogramLong<Long>> servTimeseriesByte, HistogramLong<Long> otherTimeseriesByte) {
    	if (servTimeseriesByte.size() <= 10) return;
		
    	final long SUM = servTimeseriesByte.get(TIMESERIES_TOTAL).getSum();
    	final double THRESHOLD = SUM * 0.01;
		
		Set<String> cutoff = CUtil.makeSet();
		for (Entry<String, HistogramLong<Long>> entry : servTimeseriesByte.entrySet()) {
			if (entry.getValue().getSum() < THRESHOLD) {
				cutoff.add(entry.getKey());
			}
		}
		
		if (cutoff.isEmpty()) return;
		
		for (String serv : cutoff) {
			HistogramLong<Long> timeseriesByte = servTimeseriesByte.remove(serv);
			otherTimeseriesByte.add(timeseriesByte);
		}
	}

	private void loadExtTimeseries() throws IOException {
    	System.out.println("Reading timeseries (external) ...");
    	
    	mExtRange = new Range();
        mExtIPServTimeseriesByte = CUtil.makeMap();
        mExtIPOtherTimeseriesByte = CUtil.makeMap();
        
        int i = 0;
        for (String feature : Config.INSTANCE.getFeaturePaths()) {
	        BufferedReader in = new BufferedReader(new FileReader(feature + Config.INSTANCE.getServiceTimeSeries()));
	        String line = in.readLine();
	        String prevIP = null;
	        while ((line = in.readLine()) != null) {
	            if (++i % 10000000 == 0) {
	                System.out.println((i / 1000000) + "m lines ...");
	            }
	
	            String[] split = StringUtil.trimmedSplit(line, ",");
	            //IP,sval,dval,stime,records,packets,bytes
	            String ip = split[0];
	            if (!mExtIPSet.contains(ip)) continue;
	            
	            String serv = ServFormatter.format(split[1], split[2]);
	            long time = Long.parseLong(split[3]);
//	            long records = Long.parseLong(split[4]);
//	            long packets = Long.parseLong(split[5]);
	            long bytes = Long.parseLong(split[6]);
	            mExtRange.extend(time);
	            updateTimeseries(mExtIPServTimeseriesByte, ip, serv, time, bytes);
	            updateTimeseries(mExtIPServTimeseriesByte, ip, TIMESERIES_TOTAL, time, bytes);
	
	            if (prevIP != null && !prevIP.equals(ip)) {
	            	HistogramLong<Long> other = mExtIPOtherTimeseriesByte.get(prevIP);
	            	if (other == null) {
	            		other = new HistogramLong<Long>();
	            		mExtIPOtherTimeseriesByte.put(prevIP, other);
	            	}
	            	mergeTimeseries(mExtIPServTimeseriesByte.get(prevIP), other);
	            }
	            prevIP = ip;            
	        }
	        in.close();
        }
        
        for (Entry<String, HistogramLong<Long>> entry : mExtIPOtherTimeseriesByte.entrySet()) {
        	String ip = entry.getKey();
        	Map<String, HistogramLong<Long>> servTimeseriesByte = mExtIPServTimeseriesByte.get(ip);
        	servTimeseriesByte.put(TIMESERIES_OTHER, entry.getValue());
        }
    }

	private void loadIntTimeseries() throws IOException {
		System.out.println("Reading timeseries (internal) ...");
    	
    	mIntRange = new Range();
        mIntIPServTimeseriesByte = CUtil.makeMap();
        mIntIPOtherTimeseriesByte = CUtil.makeMap();
        
        int i = 0;
        for (String feature : Config.INSTANCE.getFeaturePaths()) {
	        BufferedReader in = new BufferedReader(new FileReader(feature + Config.INSTANCE.getIntServiceTimeSeries()));
	        String line = in.readLine();
	        String prevIP = null;
	        while ((line = in.readLine()) != null) {
	            if (++i % 10000000 == 0) {
	                System.out.println((i / 1000000) + "m lines ...");
	            }
	
	            String[] split = StringUtil.trimmedSplit(line, ",");
	            //IP,sval,dval,stime,records,packets,bytes
	            String ip = split[0];
	            if (!mIntIPSet.contains(ip)) continue;
	            
	            String serv = ServFormatter.format(split[1], split[2]);
	            long time = Long.parseLong(split[3]);
//	            long records = Long.parseLong(split[4]);
//	            long packets = Long.parseLong(split[5]);
	            long bytes = Long.parseLong(split[6]);
	            mIntRange.extend(time);
	            updateTimeseries(mIntIPServTimeseriesByte, ip, serv, time, bytes);
	            updateTimeseries(mIntIPServTimeseriesByte, ip, TIMESERIES_TOTAL, time, bytes);
	
	            if (prevIP != null && !prevIP.equals(ip)) {
	            	HistogramLong<Long> other = mIntIPOtherTimeseriesByte.get(prevIP);
	            	if (other == null) {
	            		other = new HistogramLong<Long>();
	            		mIntIPOtherTimeseriesByte.put(prevIP, other);
	            	}
	            	mergeTimeseries(mIntIPServTimeseriesByte.get(prevIP), other);
	            }
	        	prevIP = ip;
	        }
	        in.close();
        }
        
        for (Entry<String, HistogramLong<Long>> entry : mIntIPOtherTimeseriesByte.entrySet()) {
        	String ip = entry.getKey();
        	Map<String, HistogramLong<Long>> servTimeseriesByte = mIntIPServTimeseriesByte.get(ip);
        	servTimeseriesByte.put(TIMESERIES_OTHER, entry.getValue());
        }
	}

	private void loadGraph() throws IOException {		
		System.out.println("Reading external-internal bipartite graph ...");
    	
		mIntExtGraph = new BipartiteGraph<String, String>();
		
        BufferedReader in = new BufferedReader(new FileReader(Config.INSTANCE.getExtIntGraphPath()));
        String line = in.readLine();
        int i = 0;
        while ((line = in.readLine()) != null) {
        	if (++i % 10000000 == 0) {
                System.out.println((i / 1000000) + "m edges ...");
            }
        	
            String[] split = StringUtil.trimmedSplit(line, ",");
            //internal,external
            String intIP = split[0];
            String extIP = split[1];
            if (!mExtIPSet.contains(extIP)) continue;
            if (!mIntIPSet.contains(intIP)) continue;
            
            mIntExtGraph.addEdge(intIP, extIP);
        }
        in.close();
	}

}