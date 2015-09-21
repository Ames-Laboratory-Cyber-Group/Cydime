package gov.ameslab.cydime.aggregate.aggregator;

import java.util.List;

import weka.core.Attribute;
import weka.core.Instance;

public interface Aggregator {

	String getName();
	
	boolean isNumeric();

	void aggregate(List<Instance> source, int srcIndex, Instance target, int targetIndex);

	Attribute makeAttribute(String name);
	
}
