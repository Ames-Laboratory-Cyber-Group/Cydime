package gov.ameslab.cydime.ranker.evaluate;

import gov.ameslab.cydime.util.CUtil;

import java.util.List;
import java.util.Set;

public class NDCG implements RankEvaluator {

	@Override
	public String getName() {
		return "nDCG";
	}

	@Override
	public double evaluate(List<String> pos, List<String> rank) {
		double sum = 0.0;
		
		Set<String> p = CUtil.makeSet(pos);
		for (int i = 0; i < rank.size(); i++) {
			String item = rank.get(i);
			if (p.contains(item)) {
				sum += getDCG(i);
			}
		}
		
		return sum / getIdealDCG(pos.size());
	}
	
	private double getIdealDCG(int posSize) {
		double sum = 0.0;
		for (int i = 0; i < posSize; i++) {
			sum += getDCG(i);
		}
		return sum;
	}

	private double getDCG(int i) {
		return 1.0 / Math.log(i + 2.0);
	}

	
}
