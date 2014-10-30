package gov.ameslab.cydime.cluster;

import java.io.IOException;

public interface Dataset {

	void writeMap(String file, int it) throws IOException;

}
