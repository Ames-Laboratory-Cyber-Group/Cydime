package gov.ameslab.cydime.graph.visualisation;

import gov.ameslab.cydime.preprocess.community.Matrix;
import gov.ameslab.cydime.util.IndexedList;
import gov.ameslab.cydime.util.ServiceParser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by maheedhar on 9/10/15.
 */
public class ReadClusters {

    String[] services = ServiceParser.SERVICES;
    String[] daysList = {"01","06","08",  "10",  "14" , "16"  ,"20" , "22",  "24" , "28"  ,"30",
            "02" , "07",  "09",  "13",  "15",  "17",  "21",  "23",  "27",  "29",  "31"};
    Integer asnSize = 0;
    Integer subnetServicesSize = 0;
    String basePath = "/home/maheedhar/cluster/07/";
    ArrayList<String> subnetServices =  new ArrayList<String>();
    HashSet<String> subnet;

    public IndexedList<String> getNodesList() throws IOException {
        //The nodes list will comprise of all the asn nodes and subnet_service nodes
        ArrayList<String> tempResult = new ArrayList<String>();
        String inPath = new String(basePath+"ipASNMap.csv");
        BufferedReader in = new BufferedReader(new FileReader(inPath));
        in.readLine();
        String line;
        HashSet<String> temp = new HashSet<String>();
        while ((line = in.readLine()) != null) {
            String[] edge = line.split(",");
            try{
                if(!edge[1].trim().equals("NA")){
                    temp.add(edge[1]);
                }
            }catch (Exception e){
                System.out.println(e);
            }
        }
        in.close();
        asnSize=temp.size();
        tempResult.addAll(temp);
        String inPath1 = new String(basePath+"netreg_nobuilding.csv");
        BufferedReader in1 = new BufferedReader(new FileReader(inPath1));
        String line1;
        in1.readLine();
        HashSet<String> temp1 = new HashSet<String>();
        while ((line1 = in1.readLine()) != null) {
            String[] edge = line1.split(",");
            try{
                temp1.add(edge[2]);
            }catch (Exception e){
                System.out.println(e);
            }
        }
        in1.close();
        subnet = temp1;
        for(String subnet : temp1){
            for(String service : services){
                tempResult.add(subnet + "__" + service);
                subnetServices.add(subnet+"__"+service);
            }
        }
        subnetServicesSize = subnetServices.size();
        IndexedList<String> result = new IndexedList<String>(tempResult);
        return result;
    }

    public Matrix<Integer> GetInteractionGroupASN(IndexedList nodeList) throws IOException{
        Matrix<Integer> edges = new Matrix<Integer>(subnetServicesSize,asnSize,0);
        for(String day : daysList){
            for( String service : services) {
                BufferedReader bw = new BufferedReader(new FileReader(basePath + "intermediateOutput/affiliation/" + service + "/intermediateOutput_affiliation_" + day + "_" + service + ".bic"));
                String line;
                bw.readLine();
                bw.readLine();
                while((line = bw.readLine()) != null){
                    String[] parts = line.split(",");
                    int size = parts.length;
                    int subnetIndex = nodeList.getIndex(parts[0]+"__"+service);
                    for(int i=2;i<size;i++){
                        int asnIndex = nodeList.getIndex(parts[i]);
                        int value = edges.get(subnetIndex,asnIndex);
                        edges.set(subnetIndex,asnIndex,value+1);
                    }
                }
                bw.close();
            }
        }
        return edges;
    }

}
