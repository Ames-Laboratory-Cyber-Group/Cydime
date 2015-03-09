package gov.ameslab.cydime.preprocess.community;

import gov.ameslab.util.CUtil;

import java.util.List;

public class LogFact {

	private List<Double> cLogFact;
	
	public LogFact() {
		cLogFact = CUtil.makeList();
		cLogFact.add(0.0);
	}
	
	public double logFact(int a) {
		if (a >= cLogFact.size()) {
			fillLogFact(a);
		}
		return cLogFact.get(a);
	}

	private void fillLogFact(int a) {
		for (int i = cLogFact.size(); i <= a; i++) {
			double prev = cLogFact.get(i - 1);
			cLogFact.add(prev + Math.log(i));
		}
	}

	public double logChoose(int a, int b) {
		return logFact(a) - logFact(b) - logFact(a - b);
	}
	
	public static void main(String[] args) {
		LogFact lf = new LogFact();
		for (int i = 0; i < 30; i++) {
			System.out.println(lf.logFact(i));
		}
	}
	
}
