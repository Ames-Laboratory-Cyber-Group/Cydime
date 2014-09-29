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

package gov.ameslab.cydime.explorer.views;

import java.awt.geom.Point2D;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;

/**
 * A plot that is selectable must implement this interface to provide a
 * mechanism for the {@link ChartPanel} to control the selecting.
 * 
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public interface Selectable {

	public interface SelectionListener {
		
		public void select(List<Integer> indexes);
		
	}
	
    /**
     * Returns <code>true</code> if the plot's domain is selectable, and
     * <code>false</code> otherwise.
     *
     * @return A boolean.
     *
     * @see #isRangeSelectable()
     */
    public boolean isDomainSelectable();

    /**
     * Returns <code>true</code> if the plot's range is selectable, and
     * <code>false</code> otherwise.
     *
     * @return A boolean.
     *
     * @see #isDomainSelectable()
     */
    public boolean isRangeSelectable();

    /**
     * Returns the orientation of the plot.
     *
     * @return The orientation.
     */
    public PlotOrientation getOrientation();
    
    /**
     * Zooms in on the domain axes.  The <code>source</code> point can be used
     * in some cases to identify a subplot for selecting.
     *
     * @param lowerPercent  the new lower bound.
     * @param upperPercent  the new upper bound.
     * @param state  the plot state.
     * @param source  the source point (in Java2D coordinates).
     *
     * @see #selectRangeAxes(double, double, PlotRenderingInfo, Point2D)
     */
    public void selectDomainAxes(double lowerPercent, double upperPercent,
                               PlotRenderingInfo state, Point2D source);

    /**
     * Zooms in on the range axes.  The <code>source</code> point can be used
     * in some cases to identify a subplot for selecting.
     *
     * @param lowerPercent  the new lower bound.
     * @param upperPercent  the new upper bound.
     * @param state  the plot state.
     * @param source  the source point (in Java2D coordinates).
     *
     * @see #selectDomainAxes(double, double, PlotRenderingInfo, Point2D)
     */
    public void selectRangeAxes(double lowerPercent, double upperPercent,
                              PlotRenderingInfo state, Point2D source);

	public void updateSelection();

	public void clearSelection();

	public void setSelectionListener(SelectionListener l);

}
