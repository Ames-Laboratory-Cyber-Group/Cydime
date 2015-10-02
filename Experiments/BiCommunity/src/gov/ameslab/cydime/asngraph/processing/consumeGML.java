package gov.ameslab.cydime.asngraph.processing;

import gov.ameslab.cydime.util.FileUtil;

import java.io.File;
import java.util.StringTokenizer;

/**
 * Created by maheedhar on 9/22/15.
 */
public class consumeGML {
    public void getNodesData(String filename){
        String contents = FileUtil.getFileContents(filename);

        StringTokenizer st = new StringTokenizer(contents);
        while (st.hasMoreTokens()){
            String token = st.nextToken();
            if(token.contains("node")){
                int id;
                String label;
                token = st.nextToken();
                if (!token.equalsIgnoreCase("[")) {
                    System.out.println("Expecting '[' and got: " + token);
                }

                token = st.nextToken();
                if (!token.equalsIgnoreCase("id")) {
                    System.out.println("Expecting 'id' and got: " + token);
                }

                // Now we should be at the name of file being linked to.
                String nodeId = st.nextToken();

                try{
                    id = Integer.parseInt(nodeId);
                }catch (NumberFormatException e){
                    System.out.println("Expecting an integer ID and got: " + nodeId);
                }

                token = st.nextToken();
                if (!token.equalsIgnoreCase("label")) {
                    System.out.println("Expecting 'label' and got: " + token);
                }

                token = st.nextToken();
                if (!token.equalsIgnoreCase(">")) {
                    System.out.println("Expecting '>' and got: " + token);
                }

                String hypertext = ""; // The text associated with this hyperlink.
            }
        }
    }

    public void getEdgesData(File src){

    }
}
