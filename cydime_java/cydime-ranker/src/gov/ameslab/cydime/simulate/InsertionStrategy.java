package gov.ameslab.cydime.simulate;

import java.util.LinkedList;
import java.util.List;

public interface InsertionStrategy {

	String getName();

	void insert(LinkedList<Alert> buffer, List<Alert> alerts);

}
