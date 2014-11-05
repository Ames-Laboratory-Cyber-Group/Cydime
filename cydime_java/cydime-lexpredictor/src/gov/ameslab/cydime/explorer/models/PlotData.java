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
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.CSVReader;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.HashMap;
import gov.ameslab.cydime.util.HashMap.ValueFactory;
import gov.ameslab.cydime.util.HistogramLong;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.models.TreeNode;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.tree.DefaultTreeModel;

/**
 * MVC Model for the scatter plot.
 * Holds a nested Map for fast retrieval of records for the tree filters.
 *
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class PlotData {

    private static final double SIGNIFICANCE_THRESHOLD = 0.2;
	
	public static final String TREE_ROOT_NAME = "ALL";
    private static final String NOT_AVAILABLE = "N/A";
	
	private DomainDatabase mDomainDB;
    private IndexedList<String> mAttributes;
    private Map<String, float[]> mIPRecord;
    
    private List<String> mExtIPs;
    private Set<String> mExtIPSet;
    private List<String> mIntIPs;
    
    private Map<String, Map<String, Map<String, Set<String>>>> mServDomainASNIPs;
    private Map<String, String> mDomainParents;
    private HistogramLong<String> mServiceCount;
    private HistogramLong<String> mDomainCount;
    private HistogramLong<String> mASNCount;
		
	public void load(DomainDatabase domainDB) throws IOException {
		mDomainDB = domainDB;
		Config.INSTANCE.setFeatureSet(Config.FEATURE_IP_DIR);
		mExtIPs = InstanceDatabase.load(Config.INSTANCE.getBasePath()).getIDs();
		mExtIPSet = CUtil.makeSet(mExtIPs);
		
		loadTable();
		
		Config.INSTANCE.setFeatureSet(Config.FEATURE_INT_DIR);
    	mIntIPs = InstanceDatabase.load(Config.INSTANCE.getBasePath()).getIDs();
    }
	
	public List<String> getAttributes() {
		return mAttributes.getList();
	}
    
	public float[][] getRecords(List<String> ips) {
		float[][] recs = new float[mAttributes.size()][ips.size()];
		for (int i = 0; i < ips.size(); i++) {
			float[] rec = mIPRecord.get(ips.get(i));
			for (int a = 0; a < rec.length; a++) {
				recs[a][i] = rec[a];
			}
		}
		return recs;
	}
	
	public List<String> getIPs(String service, String domain, String whois) {
		if (service == null) service = TREE_ROOT_NAME;
		if (domain == null) domain = TREE_ROOT_NAME;
		if (whois == null) whois = TREE_ROOT_NAME;
		
		List<String> ipList = CUtil.makeList();
		
		Map<String, Map<String, Set<String>>> servWhoisIPs = mServDomainASNIPs.get(service);
		if (servWhoisIPs == null) return ipList;
		
		Map<String, Set<String>> whoisIPs = servWhoisIPs.get(domain);
		if (whoisIPs == null) return ipList;
		
		Set<String> ips = whoisIPs.get(whois);
		if (ips == null) return ipList;
		
		return CUtil.makeList(ips);
	}
	
	private void loadTable() throws IOException {
		System.out.println("Reading plot ...");
		
        mIPRecord = CUtil.makeMap();
        mServDomainASNIPs = CUtil.makeMap();
        mDomainParents = new TreeMap<String, String>();
        
        mServiceCount = new HistogramLong<String>();
        mDomainCount = new HistogramLong<String>();
        mASNCount = new HistogramLong<String>();
        
        mAttributes = new IndexedList<String>(
        		"Bytes",
        		"Peers",
        		"Daily Regularity",
        		"Hourly Bins",
        		"Lexical Sim (Actual)",
        		"Lexical Sim (Predicted)",
        		"Semantic (Predicted)",
        		"Strength (Predicted)"
        		);
        
        CSVReader in = new CSVReader();
//        IP,service,cc,total_records,total_bytes,earliest_starttime,latest_endtime,total_peercount,total_localport,total_remoteport,ratio_local_remote_port,Bytes_Fourier0,Bytes_Fourier1,access_hours,access_days,workhour_perc,service_icmp,service_ssh,service_smtp,service_domain,service_rtsp,service_http,service_mail,service_vpn,service_OTHER,hour_0,hour_4,hour_8,hour_12,hour_16,hour_20,class
        in.add(Config.INSTANCE.getBaseNormPath() + WekaPreprocess.CSV_SUFFIX);
//        IP,service,cc,total_records,total_bytes,earliest_starttime,latest_endtime,total_peercount,total_localport,total_remoteport,ratio_local_remote_port,Bytes_Fourier0,Bytes_Fourier1,access_hours,access_days,workhour_perc,service_icmp,service_ssh,service_smtp,service_domain,service_rtsp,service_http,service_mail,service_vpn,service_OTHER,hour_0,hour_4,hour_8,hour_12,hour_16,hour_20,class
        in.add(Config.INSTANCE.getBaseNormPath() + WekaPreprocess.CSV_REPORT_SUFFIX);
//        IP,hierarchy_stack,baseNorm_stack,class
        in.add(Config.INSTANCE.getAllPredictedPath() + WekaPreprocess.CSV_SUFFIX);
//        IP,score
        in.add(Config.INSTANCE.getFinalResultPath());
        int i = 0;
        while (in.readLine()) {
        	String ip = in.get("IP");
        	String label = in.get("class");
            String serv = in.get("service");
            String domain = mDomainDB.getDomain(ip);
            String asn = mDomainDB.getASN(ip);
            String semantic = in.get(2, "hierarchy_stack");
            String strength = in.get(2, "baseNorm_stack");
            
            if (!mExtIPSet.contains(ip)) continue;
            
    		if (domain == null || domain.isEmpty()) {
            	domain = NOT_AVAILABLE;
            }
            
            if (asn == null || asn.isEmpty()) {
            	asn = NOT_AVAILABLE;
            }
            
            float[] rec = new float[] {
            		Float.parseFloat(in.get("total_bytes")),
            		Float.parseFloat(in.get("total_peercount")),
            		Float.parseFloat(in.get("Bytes_Fourier0")),
            		Float.parseFloat(in.get("access_hours")),
            		(label.equals("?")) ? 0.0f : Float.parseFloat(label),
                    Float.parseFloat(in.get(3, "score")),
            		(semantic == null) ? 0.0f : Float.parseFloat(semantic),
            		(strength == null) ? 0.0f : Float.parseFloat(strength)
            };
            
            if (!isAboveThrehold(rec)) continue;
            
            mIPRecord.put(ip, rec);
            
            for (String servPart = serv; servPart != null; servPart = getParent(servPart)) {
            	mServiceCount.increment(servPart);
            }
            
            for (String domainPart = domain; domainPart != null; domainPart = getParent(domainPart)) {
            	mDomainCount.increment(domainPart);
            }
            
            for (String asnPart = asn; asnPart != null; asnPart = getParent(asnPart)) {
            	mASNCount.increment(asnPart);
            }
            
            for (String servPart = serv; servPart != null; servPart = getParent(servPart)) {
                for (String domainPart = domain; domainPart != null; domainPart = getParent(domainPart)) {
                	for (String asnPart = asn; asnPart != null; asnPart = getParent(asnPart)) {
                		updateMap(ip, servPart, domainPart, asnPart);
                	}
                }
            }
            
            updateParents(domain, mDomainParents);
            
            if (++i % 100000 == 0) {
                System.out.println((i / 1000) + "k IPs ...");
            }
        }
        in.close();
        
        System.out.println(mIPRecord.size() + " IPs above threshold");
    }
    
	private boolean isAboveThrehold(float[] rec) {
//		return true;
		double sqrt = Math.sqrt(rec[0] * rec[0] + rec[5] * rec[5]);
		return sqrt > SIGNIFICANCE_THRESHOLD;
	}

	private void updateParents(String path, Map<String, String> parents) {
    	String parent = getParent(path);
    	while (parent != null) {
    		parents.put(path, parent);
    		path = parent;
    		parent = getParent(path);
    	}
	}

	private String getParent(String part) {
		if (TREE_ROOT_NAME.equals(part)) return null;
		
		int dot = part.indexOf(".");
		if (dot < 0) {
			return TREE_ROOT_NAME;
		} else {
			return part.substring(dot + 1);
		}
	}

	private void updateMap(String ip, String serv, String domain, String whois) {
		Map<String, Set<String>> whoisIPs = getOrMakeMap(getOrMakeMap(mServDomainASNIPs, serv), domain);
		Set<String> ips = whoisIPs.get(whois);
        if (ips == null) {
        	ips = CUtil.makeSet();
        	whoisIPs.put(whois, ips);
        }
        
        ips.add(ip);
	}
	
    private <T> Map<String, T> getOrMakeMap(Map<String, Map<String, T>> map, String key) {
    	Map<String, T> value = map.get(key);
    	if (value == null) {
    		value = CUtil.makeMap();
    		map.put(key, value);
        }
		return value;
	}
    
    private DefaultTreeModel getTreeModel(Map<String, String> parents, HistogramLong<String> count) {
    	HashMap<String, TreeNode> nodeMap = CUtil.makeMapWithInitializer(new ValueFactory<String, TreeNode>() {
			@Override
			public TreeNode make(String key) {
				return new TreeNode(key);
			}
		});
    	
    	for (Entry<String, String> entry : parents.entrySet()) {
    		String c = entry.getKey();
    		String p = entry.getValue();
    		TreeNode cNode = nodeMap.getOrMake(c);
    		cNode.setCount(count.get(c));
    		TreeNode pNode = nodeMap.getOrMake(p);
    		pNode.setCount(count.get(p));
    		pNode.add(cNode);
    	}
    	
		return new DefaultTreeModel(nodeMap.get(TREE_ROOT_NAME));
    }

	public DefaultTreeModel getDomainTree() {
		return getTreeModel(mDomainParents, mDomainCount);
	}

	public DefaultTreeModel getStarTreeFromCount(HistogramLong<String> count) {
		List<String> list = CUtil.makeList(count.keySet());
		list.remove(TREE_ROOT_NAME);
		Collections.sort(list);
		
		TreeNode root = new TreeNode(TREE_ROOT_NAME);
		root.setCount(count.get(TREE_ROOT_NAME));
		for (String node : list) {
			TreeNode c = new TreeNode(node);
			c.setCount(count.get(node));
			root.add(c);
		}
		return new DefaultTreeModel(root);
	}
	
	public DefaultTreeModel getASNTree() {
		return getStarTreeFromCount(mASNCount);
	}

	public DefaultTreeModel getServiceTree() {
		return getStarTreeFromCount(mServiceCount);
	}
	
	public int getTotalSize() {
		return mIPRecord.size();
	}

	public List<String> getIPsAboveThrehold() {
		return CUtil.makeList(mIPRecord.keySet());
	}

	public List<String> getIntIPs() {
		return mIntIPs;
	}

}
