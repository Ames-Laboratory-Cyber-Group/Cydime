package gov.ameslab.cydime.graph.visualisation;

import gov.ameslab.cydime.preprocess.community.Matrix;
import gov.ameslab.cydime.util.IndexedList;

import java.io.IOException;

/**
 * Created by maheedhar on 9/10/15.
 */
public class CreateGML {
    public static void main(String[] args) throws IOException{
        OutputGML outputGML = new OutputGML();
        ReadClusters readClusters = new ReadClusters();
        IndexedList<String> nodesList= readClusters.getNodesList();
        Matrix<Integer> edges = readClusters.GetInteractionGroupASN(nodesList);
        outputGML.save(nodesList,edges,readClusters.subnetServices,readClusters.subnet);
    }
}
