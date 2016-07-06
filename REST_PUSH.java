import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import org.apache.commons.codec.binary.Base64;

public class OdlPush {
    //private AmazonEC2Client ec2 = null;
    //private AmazonDirectConnectClient dc = null;
    String localFakeMap;
    private OdlServer ec2Client = null;
    private String topologyUri = null;
    //private Regions region = null;
    static final Logger logger = Logger.getLogger(OdlPush.class.getName());
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    
    public OdlPush(){
        
    }
    
    public String pushPropagate(String access_key_id, String secret_access_key, String mappingId, String model,String modelAddTtl, String modelReductTtl, String topologyURI, String subsystemBaseUrl) throws EJBException, Exception   {
        
        String requests="";
        String reduction_string="";
        String addition_string="";
        OntModel modelRef = ModelUtil.unmarshalOntModel(model);
        OntModel modelAdd = ModelUtil.unmarshalOntModel(modelAddTtl);
        OntModel modelReduct = ModelUtil.unmarshalOntModel(modelReductTtl);
        reduction_string=parseFlowsReduct(modelReduct,mappingId,topologyURI);
        if(reduction_string.length()>1){
            requests="Reduction\n"+reduction_string+"end_Reduct";
        }
        addition_string=parseFlows(modelAdd,topologyURI);
        
        if(addition_string.length()>1){
            requests=requests+"Addition\n"+addition_string+"end_Add";
        }
        
        return requests;
    
    }
    
    
    public void pushCommit(String access_key_id, String secret_access_key,String model,String fakeMap, String topologyURI, String subsystemBaseUrl, DriverSystemDelta aDelta) throws EJBException, Exception   {
         aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId()); // refresh
         String stringModelAdd = aDelta.getSystemDelta().getModelAddition().getTtlModel();
         String newStringModelAdd = stringModelAdd;
         
         localFakeMap=fakeMap;
         
         String reduction="";
         String addition="";
         if(model.contains("Reduction")){
             reduction=model.split("Reduction\n")[1].split("\nend_Reduct")[0];
             
         }
         if(model.contains("Addition")){
             addition=model.split("Addition\n")[1].split("\nend_Add")[0];
             
         }

         String[] json_string;
         if(!addition.isEmpty()){
            json_string=addition.split("\n");
            for(int i=0;i<json_string.length;i++){
                //subsystemBaseUrl
                URL url = new URL(String.format("http://206.196.179.141:8181/restconf/config/opendaylight-inventory:nodes/node/openflow:1/table/0/flow/0"));
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                String status[] = new String[2];
                status = this.executeHttpMethod(access_key_id, secret_access_key,url, conn, "PUT", json_string[i+2]);
                if (Integer.parseInt(status[0])!=200) {
                    throw new EJBException(String.format("Failed to push %s into %s",json_string[i+2],json_string[i]));
                }
                //String realFlowId=status[1].split(json_string[i]+"/")[1];
                String realFlowId = "openflow:1";
                String fakeFlowId=json_string[i+1];
                if(newStringModelAdd.contains(fakeFlowId)){
                    //newStringModelAdd=newStringModelAdd.replace(fakeFlowId, realFlowId);
                }
               
                i=i+2;
            }

         }
         if(!reduction.isEmpty()){
            json_string=reduction.split("\n");
            for(int i=0;i<json_string.length;i++){
                URL url = new URL(String.format(subsystemBaseUrl+"/flows/"+json_string[i]+"/"+json_string[i+1]));
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                String status[]=new String[2];
                String fakeMapMatrix[]=localFakeMap.split("\n");
                int fakeMapSize=fakeMapMatrix.length;
                for(int j=0;j<fakeMapSize;j++){
                    
                    if(fakeMapMatrix[j].contains(json_string[i+1])){
                        json_string[i+1]=fakeMapMatrix[j].split("-->>")[1].split("table-0:flow-")[1];
                        localFakeMap=localFakeMap.replaceAll(fakeMapMatrix[j],"");
                    }
                }
                
                status = this.executeHttpMethod(access_key_id, secret_access_key,url, conn, "DELETE",null);
                if (Integer.parseInt(status[0])!=204) {
                    throw new EJBException(String.format("Failed to delete %s from %s",json_string[i+1],json_string[i]));
                }
                i++;
            }
         }
         
     }
   
    
    
    private String parseFlows(OntModel modelAdd,String topologyURI){
        String requests = "";
        String query = "SELECT ?flow  WHERE {"
               + "?flow a mrs:Flow. "
               + "}";
        ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
        //r1 will have 
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            
            Resource flow = querySolution1.get("flow").asResource();
            String query2 = "SELECT ?flowrule ?rulematch WHERE {"
                     + "?flowrule mrs:value ?rulematch. "
                     + "}";
            ResultSet r2 = executeQuery(query2, emptyModel, modelAdd);
            String[] flowdata=new String[6];
            while (r2.hasNext()) {
                QuerySolution querySolution2 = r2.next();
                Resource flow2 = querySolution2.get("flowrule").asResource();
                RDFNode rulematch = querySolution2.get("rulematch");
                if(flow2.toString().equals(flow.toString()+":rule-match-0")){
                    flowdata[0]=rulematch.toString();
                    //IN_PORT
                }
                if(flow2.toString().equals(flow.toString()+":rule-match-1")){
                    flowdata[1]=rulematch.toString();
                    //ETH_TYPE

                }
                if(flow2.toString().equals(flow.toString()+":rule-match-2")){
                    flowdata[2]=rulematch.toString();
                    //ETH_SRC_MAC
                }
                if(flow2.toString().equals(flow.toString()+":rule-match-3")){
                    flowdata[3]=rulematch.toString();
                    //ETH_DST_MAC
                }
                if(flow2.toString().equals(flow.toString()+":rule-match-4")){
                    flowdata[4]=rulematch.toString();
                    //IP
                }//check because not in flow pull model
                if(flow2.toString().equals(flow.toString()+":rule-action-0")){
                    flowdata[5]=rulematch.toString();
                    //OUT_PORT
                }
            }
           ///Construct the json here  
        if (flowdata[4]==null)
            flowdata[4] = "\"10.0.0.1/24\" ";
        if (flowdata[5] ==null)
            flowdata[5] = "10";
        
        String[] json_string=new String[3];
        json_string[2]=flow.toString().split("table-0:flow-")[1]+"\n";
        json_string[0]=flow.toString().split(topologyURI+":")[1].split(":openflow-service")[0]+"\n";
  
        json_string[1]= "{\"flow\": [{\"id\":\"0\",\"match\": {\"ethernet-match\": {\"ethernet-type\":"
                + " {\"type\": "+flowdata[1]+"}},\"ipv4-source\": "+flowdata[4]+"},\"instructions\": "
                + "{\"instruction\": [{\"apply-actions\": {\"action\": [{\"order\": \"1\",\"flood-action\": {}}]},"
                + "\"order\": \"1\" }]},\"cookie_mask\": \"10\",\"out_port\": "+flowdata[5] +",\"out_group\": \"2\",\"flow-name\": \"FooXf22\","
                + "\"installHw\": \"false\",\"barrier\": \"false\",\"strict\": \"true\",\"priority\": \"2\",\"idle-timeout\": \"0\",\"hard-timeout\": \"0\","
                + "\"cookie\": \"10\",\"table_id\": \"0\"}]}";


        requests=requests+json_string[0]+json_string[2]+json_string[1];
            
        }
        return requests;
            
            
    }
    
    private String parseFlowsReduct(OntModel modelReduct,String mappingId,String topologyURI){
       String requests = "";
       String query = "SELECT ?flow WHERE {"
               + "?flow a mrs:Flow . "
               //+ "?flow mrs:type ?type . "
               //+ "?flow mrs:value ?value . "
               + "}";
       
       String mappingIdMatrix[]=mappingId.split("\n");
       int mappingIdSize=mappingIdMatrix.length;
       
       ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
        
      while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            Resource flowId = querySolution1.get("flow").asResource();
            String sFlowId=flowId.toString();
            for (int k=0;k<mappingIdSize;k++){
                if(mappingIdMatrix[k].contains(sFlowId)){
                    sFlowId=mappingIdMatrix[k].split("-->>")[1].split("\n")[0];
                    
                }
            }
            String device=sFlowId.split(topologyURI+":")[1].split(":openflow-service:")[0];
            sFlowId=sFlowId.split(":flow-")[2];
            requests=requests+device+"\n"+sFlowId+"\n";
      //}
      }
      return requests;
}
    
        private ResultSet executeQuery(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = qexec.execSelect();

        //check on reference model if the statement is not in the model addition,
        //or model subtraction
        if (!r.hasNext()) {
            qexec = QueryExecutionFactory.create(query, refModel);
            r = qexec.execSelect();
        }
        return r;
    }
private String[] executeHttpMethod(String access_key_id,String secret_access_key,URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        String username=access_key_id;
        String password=secret_access_key;
        String userPassword=username+":"+password;
        byte[] encoded=Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded=new String(encoded);
        conn.setRequestProperty("Authorization", "Basic "+stringEncoded);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        logger.log(Level.INFO, "Sending {0} request to URL : {1}", new Object[]{method, url});
        String responseCode[] = new String[2];
        responseCode[1]=null;
        responseCode[1] = conn.getHeaderField("Location");
        responseCode[0] = Integer.toString(conn.getResponseCode());
        logger.log(Level.INFO, "Response Code : {0}", responseCode[0]);

        return responseCode;
    }
    public String getFakeFlowId(){
    
    return localFakeMap;
}
}
