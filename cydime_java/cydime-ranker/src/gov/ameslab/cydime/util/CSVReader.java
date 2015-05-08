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

package gov.ameslab.cydime.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Harris Lin (harris.lin.nz at gmail.com)
 */
public class CSVReader {

	private static final Logger Log = Logger.getLogger(CSVReader.class.getName());
	
	private List<BufferedReader> mReaders;
	private List<IndexedList<String>> mHeaders;
	private List<String[]> mCurrentLines;
	
	public CSVReader() {
		mReaders = CUtil.makeList();
		mHeaders = CUtil.makeList();
		mCurrentLines = CUtil.makeList();
	}
	
	public boolean add(String file) throws IOException {
		mCurrentLines.add(null);

		File f = new File(file);
		if (f.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(f));
			mReaders.add(in);
			
			String line = in.readLine();
			String[] headers = line.split(",");
			IndexedList<String> headerList = new IndexedList<String>(headers);
			mHeaders.add(headerList);
			
			return true;
		} else {
			Log.log(Level.INFO, "File not found: {0}", file);
			
			mReaders.add(null);
			return false;
		}
	}
	
	public boolean readLine() throws IOException {
		for (int i = 0; i < mReaders.size(); i++) {
			BufferedReader in = mReaders.get(i);
			if (in == null) continue;
			
			String line = in.readLine();
			if (line == null) return false;
			
			mCurrentLines.set(i, line.split(","));
		}
		return true;
	}

	public String get(int reader, String column) {
		IndexedList<String> header = mHeaders.get(reader);
		int index = header.getIndex(column);
		if (index < 0) return null;
		return get(reader, index);
	}
	
	public String get(String column) {
		return get(0, column);
	}
	
	public String get(int reader, int column) {
		BufferedReader in = mReaders.get(reader);
		if (in == null) return null;
		
		return mCurrentLines.get(reader)[column];
	}

	public String get(int column) {
		return get(0, column);
	}
	
	public void close() throws IOException {
		for (int i = 0; i < mReaders.size(); i++) {
			BufferedReader in = mReaders.get(i);
			if (in == null) continue;
			
			in.close();
		}
	}

}
