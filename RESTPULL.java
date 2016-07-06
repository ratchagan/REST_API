import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ranjitha
 */
public class OdlServer {
    
    private static final Logger logger = Logger.getLogger(OdlRESTDriver.class.getName());
    
    
    public HashMap<String, String[][]> getOdl(String subsystemBaseUrl, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException, NullPointerException {

        HashMap<String, String[][]> paraMap =new HashMap<String, String[][]>();
        
        URL url = new URL(subsystemBaseUrl );
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String responseStr = this.executeHttpMethod(url, conn, "GET", null, access_key_id, secret_access_key);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseStr);
        int sizeNodes = jsonObject.size();
        JSONObject nodesObj = (JSONObject) jsonObject.get("nodes");
        //Gets the entire topology
        JSONArray deviceArray = (JSONArray) nodesObj.get("node");
        //gets the device 1,2,3..
        int sizeNodes2 = deviceArray.size();
        //JSONObject jsonObject1 = (JSONObject) jsonParser.parse(responseStr);       
        //System.out.println(deviceArray.toString());
        
        String device[][] = new String[sizeNodes2][5];
        
        for (int i=0;i<sizeNodes2;i++)
        {
            JSONObject deviceObj = (JSONObject) deviceArray.get(i);
            device[i][0] = (String) deviceObj.get("id");
            device[i][1] = (String) deviceObj.get("flow-node-inventory:description");
            device[i][2] = (String) deviceObj.get("flow-node-inventory:hardware");
            device[i][3] = (String) deviceObj.get("flow-node-inventory:manufacturer");
            device[i][4] = (String) deviceObj.get("flow-node-inventory:ip-address");
            
            
            JSONArray portArray = (JSONArray) deviceObj.get("node-connector");
            int portArraySize = portArray.size();
            String portSize[][] = new String[1][1] ;
            portSize [0][0]= String.valueOf(portArray.size());
            paraMap.put("portSize+"+device[i][0],portSize);
            String port[][]= new String[portArraySize][4];
            for(int o=0;o<portArraySize;o++)
            {
            JSONObject portObj = (JSONObject) portArray.get(o);
            port[o][0] = (String) portObj.get("id");
            port[o][1] = (String) portObj.get("flow-node-inventory:hardware-address");
            port[o][2] = (String) portObj.get("flow-node-inventory:port-number");
            port[o][3] = (String) portObj.get("flow-node-inventory:name");
            
            JSONArray hostArray = (JSONArray) portObj.get("address-tracker:addresses");
            String host[][] = new String[1][2];
            if(hostArray!=null)
            {
               for (int r =0;r<hostArray.size();r++)
               {
                   JSONObject hostObj = (JSONObject) hostArray.get(r);
                   host[0][0]= String.valueOf(hostObj.get("mac"));
                   host[0][1]= String.valueOf(hostObj.get("ip"));
                   
               paraMap.put("host+"+device[i][0]+"+"+port[o][0],host);
               }  
            
            }
            paraMap.put("port+"+device[i][0]+":"+o,port);
            }

            JSONArray tableArray = (JSONArray) deviceObj.get("flow-node-inventory:table");
            
            int tableSize = tableArray.size();
            
            
            String table[][] = new String[tableSize][2];
            for (int j=0;j<tableSize;j++)
            {
                JSONObject tableObj = (JSONObject) tableArray.get(j);
                int tableObjSize = tableObj.size();

                    table[j][0] = String.valueOf(tableObj.get("id"));
                    
                    if(table[j][0].equals("0"))
                    {
                
                JSONArray flowHashArray = (JSONArray) tableObj.get("flow-hash-id-map");
                
                if(flowHashArray!=null)
                {
                String flowHash[][] = new String[flowHashArray.size()][2];
                for (int k =0;k < flowHashArray.size();k++)
                {
                    JSONObject flowHashObj = (JSONObject) flowHashArray.get(k);
                    flowHash[k][0] =(String) flowHashObj.get("hash");
                    flowHash[k][0] =(String) flowHashObj.get("flow-id");
                }
                }
                JSONArray flowArray = (JSONArray) tableObj.get("flow");
                if(flowArray!= null)
                {
                int flowArraySize = flowArray.size();
                
                String flow [][]=new String[flowArraySize][9];
                for(int l =0;l<flowArraySize;l++)
                {
                    JSONObject flowObj = (JSONObject) flowArray.get(l);
                    flow[l][0] =(String) flowObj.get("id");
                    flow[l][1] =String.valueOf(flowObj.get("priority"));
                    //problem
                    flow[l][2] =String.valueOf(flowObj.get("table_id"));
                    
                    JSONObject flowInstObj1 = (JSONObject) flowObj.get("instructions");
                    if (flowInstObj1!= null)
                    {
                    JSONArray flowInstObjArray2 = (JSONArray) flowInstObj1.get("instruction");
                    for (int n=0;n<flowInstObjArray2.size();n++)
                    {
                    JSONObject flowInstObj2 = (JSONObject) flowInstObjArray2.get(n);   
                    flow[l][3] =String.valueOf(flowInstObj2.get("order"));
                    JSONObject flowInstObj3 = (JSONObject) flowInstObj2.get("apply-actions");
                    JSONArray flowInstObjArray4 = (JSONArray) flowInstObj3.get("action");
                    for (int p=0;p<flowInstObjArray4.size();p++)
                    {
                    JSONObject flowInstObj4 = (JSONObject) flowInstObjArray4.get(p);   
                    flow[l][4] =String.valueOf(flowInstObj4.get("order"));
                    JSONObject flowInstObj5 = (JSONObject) flowInstObj4.get("output-action");
                    flow[l][5] =String.valueOf(flowInstObj5.get("max-length"));
                    flow[l][6] =String.valueOf(flowInstObj5.get("output-node-connector"));
                    }
                    }
                    }

                    JSONObject flowMatchObj = (JSONObject) flowObj.get("match");
                    if (!flowMatchObj.isEmpty())
                    {
                
                    String flowMatch [][]=new String[1][5];

                    if( flowMatchObj.get("in-port")!=null)
                        flowMatch[0][0] =(String) flowMatchObj.get("in-port");
                    
                    if( flowMatchObj.get("ethernet-match")!=null)
                    {
                        JSONObject flowMatchEthr = (JSONObject) flowMatchObj.get("ethernet-match");
                        JSONObject flowMatchEthrType =(JSONObject) flowMatchEthr.get("ethernet-type");
                        flowMatch[0][1] =String.valueOf(flowMatchEthrType.get("type"));
                        
                        if( flowMatchEthr.get("ethernet-source")!=null)
                        {
                        JSONObject flowMatchEthrSrc =(JSONObject) flowMatchEthr.get("ethernet-source");
                        
                        flowMatch[0][2] =(String) flowMatchEthrSrc.get("address");
                        }
                        if( flowMatchEthr.get("ethernet-destination")!=null)
                        {
                        JSONObject flowMatchEthrDes =(JSONObject) flowMatchEthr.get("ethernet-destination");
                        flowMatch[0][3] =(String) flowMatchEthrDes.get("address");
                        }
                    }   
                    if( flowMatchObj.get("ip-match")!=null)
                        flowMatch[0][4] =(String) flowMatchObj.get("ip-match"); 
                    //}
                    
                    paraMap.put("match"+l+"+"+device[i][0],flowMatch);
                }
                    paraMap.put("flow"+l+"+"+device[i][0],flow);
                }

                paraMap.put("table+"+device[i][0]+"+"+table[j][0], table); 
                }
                
                }
                
            }
 
        paraMap.put("device"+i, device);
        }

        String Temp1[][] = paraMap.get("device1");
        String Temp4[][] = paraMap.get("flow1");
        String Temp5[][] = paraMap.get("flowmatch1");
        String Temp2[][] = paraMap.get("hostopenflow:1:0 ");
        String Temp3[][] = paraMap.get("portopenflow:2:2 ");
        

        return paraMap;
    }
    
    public String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body, String access_key_id, String secret_access_key)
            throws IOException {

        String username = access_key_id;
        String password = secret_access_key;
        String userPassword = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded = new String(encoded);

        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Basic " + stringEncoded);
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
        int responseCode = conn.getResponseCode();
        logger.log(Level.INFO, "Response Code : {0}", responseCode);

        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        return responseStr.toString();
    }
    
}
