package gov.ameslab.cydime.preprocess.community;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.IndexedList;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by maheedhar on 7/27/15.
 */
public class InputGroup {

    private String basePath ;
    private String partitionPath;

    public InputGroup(String base){
        basePath = base;
        partitionPath = basePath + "netreg_nobuilding.csv";
        basePath = base;
    }

    private static final Logger Log = Logger.getLogger(BiCommunity.class.getName());

    public HashMap<String,Integer> getAllGroupMappings(IndexedList<String> groups) throws IOException{
        Log.log(Level.INFO, "Reading Internal Groups...");
        HashMap<String,Integer> subnets = getPartitions(new File(partitionPath),groups);
//        HashMap<String,HashSet<String>> affiliations = getDeepPartitions(partitionPath);
//        HashMap<String,HashSet<String>> result= new HashMap<String,HashSet<String>>();
//        BufferedReader br = new BufferedReader(new FileReader(partitionPath));
//        String line;
//        while ((line = br.readLine()) != null) {
//            String[] edge = line.split(",");
//            //creating an array list of internal Ips for each partition
//            if (edge.length > 2) {
//                result.put(edge[1].trim(),subnets.get(edge[1].trim()));
//                result.put(edge[2].trim(), affiliations.get(edge[2].trim()));
//                HashSet<String> AminusB = minusOperation(subnets.get(edge[1].trim()), affiliations.get(edge[2].trim()));
//                if(AminusB.size() !=0){
//                    result.put(edge[1].trim()+" minus "+edge[2].trim(),AminusB);
//                }
//                HashSet<String> BminusA = minusOperation(affiliations.get(edge[2].trim()),subnets.get(edge[1].trim()));
//                if(BminusA.size() !=0){
//                    result.put(edge[2].trim() + " minus " + edge[1].trim(),BminusA);
//                }
//                HashSet<String> intersection = subnets.get(edge[1].trim());
//                intersection.retainAll(affiliations.get(edge[2].trim()));
//                if(intersection.size() !=0){
//                    result.put(edge[1].trim()+" intersection "+edge[2].trim(),intersection);
//                }
//            }
//        }
//        br.close();
        return subnets;
    }

    public HashMap<String,Integer> getPartitions(File src,IndexedList<String> groups) throws IOException {
        String line;
        HashMap<String,Integer> result= new HashMap<String,Integer>();
        BufferedReader br1 = new BufferedReader(new FileReader(src));
        while ((line = br1.readLine()) != null) {
            String[] edge = line.split(",");
            //creating an array list of internal Ips for each partition
            if (edge.length > 2) {
                result.put(edge[0].trim(),groups.getIndex(edge[1].trim()));
            }
        }
        br1.close();
        return result;
    }

    public HashMap<String,HashSet<String>> getDeepPartitions(File src) throws IOException{
        String line;
        HashMap<String,HashSet<String>> result= new HashMap<String,HashSet<String>>();
        BufferedReader br = new BufferedReader(new FileReader(src));
        while ((line = br.readLine()) != null) {
            String[] edge = line.split(",");
            //creating an array list of internal Ips for each partition
            if (edge.length > 2) {
                if(result.get(edge[2].trim())==null){
                    HashSet<String> c = new HashSet<String>();
                    c.add(edge[0]);
                    result.put(edge[2].trim(),c);
                }
                else{
                    result.get(edge[2].trim()).add(edge[0]);
                }
            }
        }
        br.close();
        return result;
    }

    public IndexedList<String> getIndexedGroups() throws IOException{
        String line;
        BufferedReader br = new BufferedReader(new FileReader(new File(partitionPath)));
        HashSet<String> internalGroupList= new HashSet<String>();
        br.readLine();
        while ((line = br.readLine()) != null) {
            String[] edge = line.split(",");
            if (edge.length > 2) {
                internalGroupList.add(edge[1].trim());
            }
        }
        br.close();
        IndexedList<String> groups= new IndexedList<String>(internalGroupList);
        return groups;
    }

    public HashMap<String,String> readExternalIps() throws IOException{
        Log.log(Level.INFO, "Loading the external Ips host names");
        HashMap<String,String> hostNames = new HashMap<String,String>();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(new File(basePath+"ipHostMap.csv")));
        while ((line = br.readLine()) != null) {
            String[] edge = line.split(",");
            if(edge[1].equals("NA")){
                hostNames.put(edge[0],edge[0]);
            }else{
                hostNames.put(edge[0], edge[1]);
            }
        }
        br.close();
        return hostNames;
    }

    public HashMap<Integer,Integer> readASNMap(IndexedList<String> extIpList,IndexedList<Integer> asnList) throws IOException{
        HashMap<Integer,Integer> result = new HashMap<Integer,Integer>();
        String inPath = new String(basePath+"ipASNMap.csv");
        BufferedReader in = new BufferedReader(new FileReader(inPath));
        String line;
        while ((line = in.readLine()) != null) {
            String[] edge = line.split(",");
            int index = extIpList.getIndex(edge[0]);
            if(index != -1){
                try{
                    result.put(index,asnList.getIndex(Integer.parseInt(edge[1])));//yuck
                }catch (NumberFormatException e){

                }
            }
        }
        return result;
    }

    public HashMap<Integer,String> getASNNumberToASNNameMap() throws IOException{
        HashMap<Integer,String> result = new HashMap<Integer,String>();
        String inPath = new String(basePath+"ipASNMap.csv");
        BufferedReader in = new BufferedReader(new FileReader(inPath));
        String line;
        while ((line = in.readLine()) != null) {
            String[] edge = line.split(",");
            try{
                result.put(Integer.parseInt(edge[1]),edge[2]);
            }catch (NumberFormatException e){
                //since sometimes the ASN number field is "NA"
            }
            catch (ArrayIndexOutOfBoundsException e){
               //since sometimes the ASN number might not have a name and hence edge[2] causes this exception
                try{
                    result.put(Integer.parseInt(edge[1]),edge[1]);
                }catch (ArrayIndexOutOfBoundsException ex){

                }
            }
        }
        return result;
    }

    public IndexedList<Integer> getASNList() throws IOException{
        Set<Integer> asnSet = CUtil.makeSet();
        String inPath = new String(basePath+"ipASNMap.csv");
        BufferedReader in = new BufferedReader(new FileReader(inPath));
        String line;
        while ((line = in.readLine()) != null) {
            String[] edge = line.split(",");
            try {
                asnSet.add(Integer.parseInt(edge[1]));
            }catch (NumberFormatException e){

            }catch (ArrayIndexOutOfBoundsException e){

            }
        }
        return new IndexedList<Integer>(asnSet);
    }


}
