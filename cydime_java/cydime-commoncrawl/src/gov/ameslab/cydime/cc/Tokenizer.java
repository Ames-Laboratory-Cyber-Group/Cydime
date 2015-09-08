package gov.ameslab.cydime.cc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;

public class Tokenizer {
	
	private static boolean PRINT = true;
	private static int RUNS = 1;

	public static void main(String[] args) throws IOException {
		String doc = FileUtils.readFileToString(new File("data/www.ameslab.gov"));
//		System.out.println(doc);
		
		long time = System.currentTimeMillis();
		for (int i = 0; i < RUNS; i++) {
			tokStream("data/www.ameslab.gov");
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);
		
		
		time = System.currentTimeMillis();
		for (int i = 0; i < RUNS; i++) {
			tok1(doc);
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);
		
		time = System.currentTimeMillis();
		for (int i = 0; i < RUNS; i++) {
			tok2(doc);
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);
		
		time = System.currentTimeMillis();
		for (int i = 0; i < RUNS; i++) {
			tok3(doc);
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);
		
		time = System.currentTimeMillis();
		for (int i = 0; i < RUNS; i++) {
			tok4(doc);
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);
	}

	private static void tokStream(String file) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		List<String> strs = new ArrayList<String>();
		StreamTokenizer tok = new StreamTokenizer(in);
		tok.resetSyntax();
		tok.lowerCaseMode(true);
		tok.wordChars('A', 'Z');
		tok.wordChars('a', 'z');
		tok.wordChars('\u00A0', '\u00FF');
		tok.whitespaceChars('\u0000', '\u0020');
		tok.eolIsSignificant(false);
		while (true) {
			int t = tok.nextToken();
			if (t == StreamTokenizer.TT_WORD) {
				strs.add(tok.sval);
			} else if (t == StreamTokenizer.TT_EOF) {
				break;
			}
		}
		
		if (PRINT) System.out.println(strs.size() + " " + strs);
	}

	private static void tok4(String doc) {
		String[] split = doc.split("\\W");
		for (int i = 0; i < split.length; i++) {
			String str = split[i];
		}
		
		if (PRINT) System.out.println(split.length + " " + Arrays.toString(split));
	}

	private static void tok3(String doc) {
		String[] split = doc.split("[\\p{IsPunctuation}\\p{IsWhite_Space}]+");
		for (int i = 0; i < split.length; i++) {
			String str = split[i];
		}
		
		if (PRINT) System.out.println(split.length + " " + Arrays.toString(split));
	}

	private static void tok2(String doc) {
		String[] split = doc.split("\\s");
		for (int i = 0; i < split.length; i++) {
			String str = split[i];
		}
		
		if (PRINT) System.out.println(split.length + " " + Arrays.toString(split));
	}

	private static void tok1(String doc) {
		StringTokenizer tokenizer = new StringTokenizer(doc);
		List<String> strs = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			String str = tokenizer.nextToken();
			strs.add(str);
		}
		
		if (PRINT) System.out.println(strs.size() + " " + strs);
	}

}
