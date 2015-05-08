package gov.ameslab.cydime.ranker.evaluate;

import java.util.List;

public interface RankEvaluator {

	String getName();
	double evaluate(List<String> pos, List<String> rank);

}
