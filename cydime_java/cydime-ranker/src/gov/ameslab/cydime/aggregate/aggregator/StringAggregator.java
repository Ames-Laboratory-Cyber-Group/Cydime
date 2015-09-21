package gov.ameslab.cydime.aggregate.aggregator;

import java.util.List;

import weka.core.Attribute;

public abstract class StringAggregator implements Aggregator {

	@Override
	public boolean isNumeric() {
		return false;
	}
	
	@Override
	public Attribute makeAttribute(String name) {
		return new Attribute(name + "_" + getName(), (List<String>) null);
	}
	
}
