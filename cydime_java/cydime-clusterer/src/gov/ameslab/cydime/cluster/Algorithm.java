package gov.ameslab.cydime.cluster;

import java.io.IOException;


public interface Algorithm {

	String getName();
	Dataset run(String graphFile, double param) throws IOException;
	
}
