package gov.ameslab.cydime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Snort {

	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		String line;
		while ((line = in.readLine()) != null) {
			String[] split = line.split(" ");
			
			System.out.print(split[0].substring(6, 14));
			System.out.print(",");
			
			int p = line.indexOf("Priority: ");
			System.out.print(line.charAt(p + "Priority: ".length()));
			System.out.print(",");
			
			String src = getIP(split[split.length - 3]);
			String tar = getIP(split[split.length - 1]);
			if (src.startsWith("147.155.")) {
				System.out.println(tar);
			} else {
				System.out.println(src);
			}
		}
		in.close();
	}

	private static String getIP(String s) {
		int i = s.indexOf(":");
		if (i < 0) return s;
		else return s.substring(0, i);
	}
	
}
