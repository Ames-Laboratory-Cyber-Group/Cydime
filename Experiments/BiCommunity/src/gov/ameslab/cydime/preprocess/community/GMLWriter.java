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

package gov.ameslab.cydime.preprocess.community;

import gov.ameslab.cydime.util.Histogram;
import gov.ameslab.cydime.util.IndexedList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class GMLWriter {
	
	public static void write(String file, Matrix<Double> matrix, Histogram<Integer> intWeight, Histogram<Integer> extWeight) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("graph"); out.newLine();
		out.write("["); out.newLine();
		
		IndexedList<Integer> intLabels = new IndexedList<Integer>(intWeight.keySet());
		IndexedList<Integer> extLabels = new IndexedList<Integer>(extWeight.keySet());
		
		for (int i = 0; i < intLabels.size(); i++) {
			int iLabel = intLabels.get(i);
			double w = intWeight.get(iLabel);
			w = Math.log(w + 1.0);
			out.write("node"); out.newLine();
			out.write("["); out.newLine();
			out.write("id " + (i + 1)); out.newLine();
			out.write("label int" + iLabel); out.newLine();
			out.write("Weight \"" + w + "\""); out.newLine();
			out.write("Side 0"); out.newLine();
			out.write("]"); out.newLine();
		}
		
		for (int e = 0; e < extLabels.size(); e++) {
			int eLabel = extLabels.get(e);
			double w = extWeight.get(eLabel);
			w = Math.log(w + 1.0);
			out.write("node"); out.newLine();
			out.write("["); out.newLine();
			out.write("id " + (e + 1 + intLabels.size())); out.newLine();
			out.write("label ext" + eLabel); out.newLine();
			out.write("Weight \"" + w + "\""); out.newLine();
			out.write("Side 1"); out.newLine();
			out.write("]"); out.newLine();
		}
		
		out.newLine();
		for (Entry<Integer, Map<Integer, Double>> ijMap : matrix.getMatrix().entrySet()) {
			int i = ijMap.getKey();
			for (Entry<Integer, Double> jMap : ijMap.getValue().entrySet()) {
				int j = jMap.getKey();
				double w = jMap.getValue();
				w = Math.log(w + 1.0);
				out.write("edge"); out.newLine();
				out.write("["); out.newLine();
				out.write("source " + (intLabels.getIndex(i) + 1)); out.newLine();
				out.write("target " + (extLabels.getIndex(j) + 1 + intLabels.size())); out.newLine();
				out.write("value " + w); out.newLine();
				out.write("]"); out.newLine();
			}
		}
		
		out.write("]"); out.newLine();
		out.close();
	}

}
