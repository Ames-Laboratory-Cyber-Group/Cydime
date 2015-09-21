package gov.ameslab.cydime.aggregate.aggregator;

import weka.core.Attribute;

public abstract class NumericAggregator implements Aggregator {

	@Override
	public boolean isNumeric() {
		return true;
	}
	
	@Override
	public Attribute makeAttribute(String name) {
		return new Attribute(name + "_" + getName());
	}
	
}
