package gov.ameslab.cydime.graph.visualisation;

import gov.ameslab.cydime.preprocess.community.InputGroup;
import gov.ameslab.cydime.preprocess.community.Matrix;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.MapSet;
import gov.ameslab.cydime.util.ServiceParser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by maheedhar on 9/9/15.
 */
public class OutputGML {

    public void save(IndexedList nodeList,Matrix edges,ArrayList<String> subnetServices,HashSet<String> subnets) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter("asnGraph.gml"));
        int tempindex=0;
        try {
            edges.updateTranspose();
            edges.updateAdjacencyArrays();
            out.write("graph");
            out.newLine();
            out.write("[");
            out.newLine();
            out.write("directed 0");
            out.newLine();
            int numberOfNodes = nodeList.size();
            HashMap<Integer,String> asnNames = new InputGroup("/home/maheedhar/cluster/07/").getASNNumberToASNNameMap();
            for (int i = 0; i < numberOfNodes; i++) {
                out.write("node");
                out.newLine();
                out.write("[");
                out.newLine();
                out.write("id " + i);
                out.newLine();
                String asnName = null;
                try{
                    asnName = asnNames.get(Integer.parseInt(nodeList.get(i).toString()));
                }catch(NumberFormatException e){

                }
                if(asnName != null){
                    out.write("label \"" + asnName + "\"");
                }else {
                    out.write("label \"" + nodeList.get(i) + "\"");
                }
                out.newLine();
                out.write("]");
                out.newLine();
            }
            for (String subnet : subnetServices) {
                int subnetIndex = nodeList.getIndex(subnet);
                if(edges.getNeighborListOfI(subnetIndex) != null) {
                    for (Integer asnIndex : edges.getNeighborListOfI(subnetIndex)) {
                        if ((Integer) edges.get(subnetIndex, asnIndex) != 0) {
                            out.write("edge");
                            out.newLine();
                            out.write("[");
                            out.newLine();
                            out.write("source " + subnetIndex);
                            out.newLine();
                            out.write("target " + asnIndex);
                            out.newLine();
                            out.write("value " + edges.get(subnetIndex, asnIndex));
                            out.newLine();
                            out.write("]");
                            out.newLine();
                        }
                    }
                }
            }
//            for (String sub : subnets) {
//                for (String service1 : ServiceParser.SERVICES) {
//                    String subService1 = sub + "__" + service1;
//                    int asnIndex1 = nodeList.getIndex(subService1);
//                    for (String service2 : ServiceParser.SERVICES) {
//                        if (!service1.equals(service2)) {
//                            String subService2 = sub + "__" + service2;
//                            int asnIndex2 = nodeList.getIndex(subService2);
//                            out.write("edge");
//                            out.newLine();
//                            out.write("[");
//                            out.newLine();
//                            out.write("source " + asnIndex1);
//                            out.newLine();
//                            out.write("target " + asnIndex2);
//                            out.newLine();
//                            out.write("value " + 60);
//                            out.newLine();
//                            out.write("]");
//                            out.newLine();
//                        }
//                    }
//                }
//            }
            out.write("]");
            out.close();
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(edges.get(1,1));
            out.close();
        }
    }



}
