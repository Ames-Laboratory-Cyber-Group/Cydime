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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FileUtil {
	
	public static void copy(String file, String newFile) throws IOException {
		Path path = FileSystems.getDefault().getPath(file);
		Path newPath = FileSystems.getDefault().getPath(newFile);
		Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
	}

	public static List<String> readFile(String file, boolean hasHeader) throws IOException {
		List<String> result = CUtil.makeList();
		BufferedReader in = new BufferedReader(new FileReader(file));
		if (hasHeader) {
			in.readLine();
		}
		
		String line;
		while ((line = in.readLine()) != null) {
			result.add(line);
		}
		in.close();
		return result;
	}

	public static void writeFile(String file, List<String> data) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		for (String line : data) {
			out.write(line);
			out.newLine();
		}
		out.close();
	}

	public static Map<String, String> readCSV(String file, boolean toLowerCase) throws IOException {
		return readCSV(file, 1, toLowerCase);
	}
	
	public static Map<String, String> readCSV(String file, int index, boolean toLowerCase) throws IOException {
		return readCSV(file, 0, index, toLowerCase, false);
	}
	
	public static Map<String, String> readCSV(String file, int key, int value, boolean toLowerCase, boolean hasHeader) throws IOException {
		Map<String, String> map = CUtil.makeMap();
		BufferedReader in = new BufferedReader(new FileReader(file));
		if (hasHeader) {
			in.readLine();
		}
		
		String line = null;
		while ((line = in.readLine()) != null) {
			if (toLowerCase) {
				line = line.toLowerCase();
			}
			
			String[] split = StringUtil.trimmedSplit(line, ",");
			map.put(split[key], split[value]);
		}
		in.close();
		return map;
	}

	public static void writeCSV(String file, Map<String, String> csv) throws IOException {
		writeCSV(file, csv, false);
	}

	public static void writeCSV(String file, Map<String, String> csv, boolean doSwapColumns) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		if (doSwapColumns) {
			for (Entry<String, String> entry : csv.entrySet()) {
				out.write(entry.getValue());
				out.write(",");
				out.write(entry.getKey());
				out.newLine();
			}
		} else {
			for (Entry<String, String> entry : csv.entrySet()) {
				out.write(entry.getKey());
				out.write(",");
				out.write(entry.getValue());
				out.newLine();
			}
		}
		
		out.close();
	}
	
}
