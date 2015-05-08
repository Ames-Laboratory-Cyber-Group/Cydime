package gov.ameslab.cydime.ranker.evaluate;

import gov.ameslab.cydime.util.CUtil;

import java.util.List;
import java.util.Set;

public class AveragePrecision implements RankEvaluator {

	@Override
	public String getName() {
		return "AP";
	}

	@Override
	public double evaluate(List<String> pos, List<String> rank) {
		int posCount = 0;
		double sum = 0.0;
		
		Set<String> p = CUtil.makeSet(pos);
		for (int i = 0; i < rank.size(); i++) {
			String item = rank.get(i);
			if (p.contains(item)) {
				posCount++;
				sum += posCount / (i + 1.0);
			}
		}
		return sum / pos.size();
	}

}
