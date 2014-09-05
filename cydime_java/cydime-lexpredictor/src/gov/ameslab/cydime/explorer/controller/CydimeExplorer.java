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

package gov.ameslab.cydime.explorer.controller;
import gov.ameslab.cydime.explorer.views.PlotFrame.DataSelectionListener;
import gov.ameslab.cydime.model.DomainDatabase;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.HistogramLong;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 *
 * @author htlin
 */
public class CydimeExplorer {
    
    public CydimeExplorer(String featurePath) {
    	Config.INSTANCE.setParam(featurePath);
	}

	private gov.ameslab.cydime.explorer.views.PlotFrame mPlotFrame = new gov.ameslab.cydime.explorer.views.PlotFrame();
	private gov.ameslab.cydime.explorer.views.StatFrame mStatFrame = new gov.ameslab.cydime.explorer.views.StatFrame();
    private gov.ameslab.cydime.explorer.models.PlotData mPlotData = new gov.ameslab.cydime.explorer.models.PlotData();
    private gov.ameslab.cydime.explorer.models.StatData mStatData = new gov.ameslab.cydime.explorer.models.StatData();
	private Set<String> mCurrentExtIPs = CUtil.makeSet();
	private Set<String> mCurrentIntIPs = CUtil.makeSet();
    
    public void init() throws IOException {
    	DomainDatabase domainDB = DomainDatabase.load();
    	mPlotData.load(domainDB);
    	mStatData.load(domainDB, mPlotData.getIPsAboveThrehold(), mPlotData.getIntIPs());
    	
    	System.out.println("Initializing GUI ...");
        mPlotFrame.setTotalIPs(mPlotData.getTotalSize());
        mPlotFrame.setAxes(mPlotData.getAttributes(), 0, 5);
        
        mPlotFrame.setDataSelectionListener(new DataSelectionListener() {
			@Override
			public void selectData(List<String> extIPs) {
				mCurrentExtIPs = CUtil.makeSet(extIPs);
				mStatFrame.updateExtIPRecords(mStatData.getExtIPRecords(extIPs));
				mCurrentIntIPs = mStatData.getIntIPNeighborsOf(extIPs);
				mStatFrame.updateIntIPRecords(mStatData.getIntIPRecords(mCurrentIntIPs));
			}        	
        });
        
        mPlotFrame.addFilterListener(new TreeSelectionListener() {
        	@Override
			public void valueChanged(TreeSelectionEvent e) {
                updateIPs();
			}
        });
        mPlotFrame.loadFilters(mPlotData.getServiceTree(), mPlotData.getDomainTree(), mPlotData.getWhoisTree());
        
        mStatFrame.setExtTimeRange(mStatData.getExtRange());
        mStatFrame.setIntTimeRange(mStatData.getIntRange());
        mStatFrame.addExtIPListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                
                List<String> ips = mStatFrame.getSelectedExtIPs();
                if (ips.isEmpty() || ips.size() > 1) {
                	mStatFrame.clearExtTimeseries();
                } else {
                	String ip = ips.get(0);
                    Map<String, HistogramLong<Long>> servTimeseriesByte = mStatData.getExtServTimeseriesByte(ip);
                    mStatFrame.updateExtTimeseries(servTimeseriesByte);
                }
            }
        });
        mStatFrame.addIntIPListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                
                List<String> ips = mStatFrame.getSelectedIntIPs();
                if (ips.isEmpty() || ips.size() > 1) {
                	mStatFrame.clearIntTimeseries();
                } else {
                	String ip = ips.get(0);
                    Map<String, HistogramLong<Long>> servTimeseriesByte = mStatData.getIntServTimeseriesByte(ip);
                    mStatFrame.updateIntTimeseries(servTimeseriesByte);
                }
            }
        });
        mStatFrame.addFilterExtIntActionPerformedListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> extIPs = mStatFrame.getSelectedExtIPs();
				Set<String> intIPs = mStatData.getIntIPNeighborsOf(extIPs);
				intIPs.retainAll(mCurrentIntIPs);
				mStatFrame.updateIntIPRecords(mStatData.getIntIPRecords(intIPs));
			}
        });
        mStatFrame.addFilterIntExtActionPerformedListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> intIPs = mStatFrame.getSelectedIntIPs();
				Set<String> extIPs = mStatData.getExtIPNeighborsOf(intIPs);
				extIPs.retainAll(mCurrentExtIPs);
				mStatFrame.updateExtIPRecords(mStatData.getExtIPRecords(extIPs));
			}
        });
        
        updateIPs();
        
        mStatFrame.setVisible(true);
        mPlotFrame.setVisible(true);
    }

    private void updateIPs() {
    	String service = mPlotFrame.getSelectedService();
		String domain = mPlotFrame.getSelectedDomain();
        String whois = mPlotFrame.getSelectedWhois();
        List<String> ips = mPlotData.getIPs(service, domain, whois);
        float[][] records = mPlotData.getRecords(ips);
        mPlotFrame.updatePlot(ips, records);
	}
    
    @SuppressWarnings("CallToThreadDumpStack")
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            new CydimeExplorer(args[0]).init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
