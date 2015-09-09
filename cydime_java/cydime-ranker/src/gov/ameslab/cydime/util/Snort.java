package gov.ameslab.cydime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Snort {

	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		String line;
		while ((line = in.readLine()) != null) {
			StringBuilder buf = new StringBuilder();
			String[] split = line.split(" ");
			
			buf.append(split[0].substring(6, 14))
				.append(",");
			
			int p = line.indexOf("Priority: ");
			buf.append(line.charAt(p + "Priority: ".length()))
				.append(",");
			
			String src = getIP(split[split.length - 3]);
			String tar = getIP(split[split.length - 1]);
			String ip;
			if (src.startsWith("147.155.")) {
				ip = tar;
			} else {
				ip = src;
			}
			
			if (ip.indexOf(".") == -1) continue;
			
			buf.append(ip);
			System.out.println(buf);
		}
		in.close();
	}

	private static String getIP(String s) {
		int i = s.indexOf(":");
		if (i < 0) return s;
		else return s.substring(0, i);
	}
	
}
