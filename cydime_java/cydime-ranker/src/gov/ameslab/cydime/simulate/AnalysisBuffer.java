package gov.ameslab.cydime.simulate;

import java.util.List;

public interface AnalysisBuffer {

	String getName();
	
	int size();

	boolean isEmpty();
	
	void insert(List<Alert> alerts);

	Alert pop();

	AnalysisBuffer makeNew();

	
}
