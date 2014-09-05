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

package gov.ameslab.cydime.convert.metric;

import cc.mallet.types.Metric;
import cc.mallet.types.SparseVector;

public abstract class Divergence implements Metric {

	protected int mSize;

	public Divergence(int size) {
		mSize = size;
	}

	@Override
	public double distance(SparseVector a, SparseVector b) {
		return (kl(a, b) + kl(b, a)) / 2.0;
	}
	
	private double kl(SparseVector a, SparseVector b) {
		double LAPLACE = 1.0 / mSize;
		double dist = 0;
		
		if (a == null || b == null) {
		    throw new IllegalArgumentException("Distance from a null vector is undefined.");
		}

		int leftLength = a.numLocations();
		int rightLength = b.numLocations();
		int leftIndex = 0;
		int rightIndex = 0;
		int leftFeature, rightFeature;

		// We assume that features are sorted in ascending order.
		// We'll walk through the two feature lists in order, checking
		//  whether the two features are the same.
		while (leftIndex < leftLength && rightIndex < rightLength) {
			leftFeature = a.indexAtLocation(leftIndex);
			rightFeature = b.indexAtLocation(rightIndex);

			double vA = LAPLACE;
			double vB = LAPLACE;
			if (leftFeature < rightFeature) {
				vA += a.valueAtLocation(leftIndex);
				leftIndex++;
			} else if (leftFeature == rightFeature) {
				vA += a.valueAtLocation(leftIndex);
				vB += b.valueAtLocation(rightIndex);
				leftIndex++;
				rightIndex++;
			} else {
				vB += b.valueAtLocation(rightIndex);
				rightIndex++;
			}

			dist += vA * (Math.log(vA) - Math.log(vB));
		}

		// Pick up any additional features at the end of the two lists.
		while (leftIndex < leftLength) {
			double vA = LAPLACE + a.valueAtLocation(leftIndex);
			double vB = LAPLACE;
			dist += divergence(vA, vB);
			leftIndex++;
		}

		while (rightIndex < rightLength) {
			double vA = LAPLACE;
			double vB = LAPLACE + b.valueAtLocation(rightIndex);
			dist += divergence(vA, vB);
			rightIndex++;
		}

		return dist;
	}

	protected abstract double divergence(double vA, double vB);

}
