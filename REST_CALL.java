import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class OdlModelBuilder {
    
    public static OntModel createOntology(String topologyURI, String subsystemBaseUrl, String srrgFile, String mappingId, String access_key_id, String secret_access_key)
            throws IOException, ParseException {

        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //set all the model prefixes"

        model.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());

        //add SRRG
        model.setNsPrefix("sna", Sna.getURI());

        //set the global properties
        Property hasNode = Nml.hasNode;
        Property hasBidirectionalPort = Nml.hasBidirectionalPort;
        Property hasService = Nml.hasService;
        Property providesVM = Mrs.providesVM;
        Property type = Mrs.type;
        Property providedByService = Mrs.providedByService;
        Property providesBucket = Mrs.providesBucket;
        Property providesRoute = Mrs.providesRoute;
        Property providesSubnet = Mrs.providesSubnet;
        Property providesVPC = Mrs.providesVPC;
        Property providesVolume = Mrs.providesVolume;
        Property routeFrom = Mrs.routeFrom;
        Property routeTo = Mrs.routeTo;
        Property nextHop = Mrs.nextHop;
        Property value = Mrs.value;
        Property hasBucket = Mrs.hasBucket;
        Property hasVolume = Mrs.hasVolume;
        Property hasTopology = Nml.hasTopology;
        Property targetDevice = model.createProperty(model.getNsPrefixURI("mrs") + "target_device");
        Property hasRoute = Mrs.hasRoute;
        Property hasTag = Mrs.hasTag;
        Property hasNetworkAddress = Mrs.hasNetworkAddress;
        Property providesRoutingTable = model.createProperty(model.getNsPrefixURI("mrs") + "providesRoutingTable");
        Property hasFlow = Mrs.hasFlow;
        Property providesFlowTable = Mrs.providesFlowTable;
        Property providesFlow = Mrs.providesFlow;
        Property flowMatch = Mrs.flowMatch;
        Property flowAction = Mrs.flowAction;
        
        Property locatedAt = Nml.locatedAt;

        //set the global resources
        Resource route = Mrs.Route;
        Resource hypervisorService = Mrs.HypervisorService;
        Resource virtualCloudService = Mrs.VirtualCloudService;
        Resource routingService = Mrs.RoutingService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;
        Resource vlan = model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
        Resource networkAddress = Mrs.NetworkAddress;
        Resource switchingSubnet = Mrs.SwitchingSubnet;
        Resource switchingService = Mrs.SwitchingService;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;
        Resource onosTopology = RdfOwl.createResource(model, topologyURI, topology);
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource routingTable = Mrs.RoutingTable;
        Resource openflowService = Mrs.OpenflowService;
        Resource flowTable = Mrs.FlowTable;
        Resource flow = Mrs.Flow;
        Resource flowRule = Mrs.FlowRule;
        
        Resource location = Nml.Location;

        //SRRG declare, only SRRG is included here currently
        Resource SRRG = Sna.SRRG;
        Property severity = Sna.severity;
        Property occurenceProbability = Sna.occurenceProbability;

        String mappingIdMatrix[]=mappingId.split("\n");
        int mappingIdSize=mappingIdMatrix.length;
        
        OdlServer onos = new OdlServer();
        HashMap<String, String[][]> res;
        res = new HashMap<>();
        res = onos.getOdl(subsystemBaseUrl, access_key_id, secret_access_key);
        //String device[][] = onos.getOdl(subsystemBaseUrl, access_key_id, secret_access_key);
        
        String device[][] = res.get("device1");
        int deviceSize = device.length;
        
        for (int i = 0; i < deviceSize; i++){
            
            Resource resNode = RdfOwl.createResource(model, topologyURI + ":" + device[i][0], node);
            model.add(model.createStatement(onosTopology, hasNode, resNode));
            Resource resOpenFlow = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service", openflowService);
            model.add(model.createStatement(resNode, hasService, resOpenFlow));
            
            //if (device[i][2].equals("Open vSwitch")  )
            {
                for(int j =0;j<10;j++)
                {
               
                String port[][] = res.get("port+"+device[i][0]+":"+j);
                if (port!=null)
                {
                Resource resPort = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":port-" + port[j][0], biPort);
                model.add(model.createStatement(resNode, hasBidirectionalPort, resPort));
                
                model.add(model.createStatement(resOpenFlow, hasBidirectionalPort, resPort));
                
               // }
            //}
            
            //for(String Key : res.keySet()){
                //table id 0 to 256 
            String table[][]=res.get("table+"+device[i][0]+"+0");
            for(int k =0;k<3;k++)
            {
            String deviceFlows[][]= res.get("flow"+k+"+"+device[i][0]);
           
            if (deviceFlows != null)
            {    
            Resource resFlowTable = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" +"0", flowTable);
                    model.add(model.createStatement(resOpenFlow, providesFlowTable, resFlowTable));
            if(deviceFlows[k][0]!=null & device[i][0]!=null)
            {
            Resource resFlow = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + "0" + ":flow-" + deviceFlows[k][0], flow);
                    model.add(model.createStatement(resFlowTable, providesFlow, resFlow));
                    
                   
            /*
                    Resource resFlowTable = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0], flowTable);
                    model.add(model.createStatement(resOpenFlow, providesFlowTable, resFlowTable));
                    
            Resource resFlow = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0], flow);
                    model.add(model.createStatement(resFlowTable, providesFlow, resFlow));
                    */
                    
            /*Resource resFlowRule0 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-0", flowRule);
            Resource resFlowRule1 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-1", flowRule);
            Resource resFlowRule2 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-2", flowRule);
            Resource resFlowRule3 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-3", flowRule);
            Resource resFlowRule4 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-4", flowRule);
            Resource resFlowAction = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-action-0", flowRule);
            */
            for(int l =0;l<5;l++)
            {
            String flowmatch[][] = res.get("match"+l+"+"+device[i][0]);
            if(flowmatch!=null)
            {
            //Resource resFlowRule4 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-4", flowRule);
            //Resource resFlowAction = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-action-0", flowRule);
            
            if(flowmatch[0][0]!=null)
            {
            Resource resFlowRule0 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-0", flowRule);
            
            model.add(model.createStatement(resFlow, flowMatch, resFlowRule0));
            model.add(model.createStatement(resFlowRule0, type, "IN_PORT"));
            model.add(model.createStatement(resFlowRule0, value, flowmatch[0][0]));
            }
            if(flowmatch[0][1]!=null)
            {
            Resource resFlowRule1 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-1", flowRule);
           
            model.add(model.createStatement(resFlow, flowMatch, resFlowRule1));
            model.add(model.createStatement(resFlowRule1, type, "ETH_TYPE"));
            model.add(model.createStatement(resFlowRule1, value, flowmatch[0][1]));
            }
                    //flowRule1: ETH_SRC_MAC
            if(flowmatch[0][2]!=null)
            {
            Resource resFlowRule2 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-2", flowRule);
            
            model.add(model.createStatement(resFlow, flowMatch, resFlowRule2));
            model.add(model.createStatement(resFlowRule2, type, "ETH_SRC_MAC"));
            model.add(model.createStatement(resFlowRule2, value, flowmatch[0][2]));
            }
                    //flowRule2: ETH_DST_MAC
            if(flowmatch[0][3]!=null)
            {
            Resource resFlowRule3 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-3", flowRule);
            
            model.add(model.createStatement(resFlow, flowMatch, resFlowRule3));
            model.add(model.createStatement(resFlowRule3, type, "ETH_DST_MAC"));
            model.add(model.createStatement(resFlowRule3, value, flowmatch[0][3]));            
            }
            
            if(flowmatch[0][4]!=null)
            {
            Resource resFlowRule4 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + table[105][0] + ":flow-" + deviceFlows[k][0] + ":rule-match-4", flowRule);
            
            model.add(model.createStatement(resFlow, flowMatch, resFlowRule4));
            model.add(model.createStatement(resFlowRule4, type, "IP"));
            model.add(model.createStatement(resFlowRule4, value, flowmatch[0][4]));            
            }
            //add ip and vlan, create REST call for vlan, increase flowmatch matrix size from 4-->5
            }//device flows are null 
            }
            }
            }

//for(int m=0;m<2;m++){
            // find a way to determine number of ports on each device    
            if(j<10 & i <deviceSize)
            {
            String temp ="host+"+device[i][0]+"+"+port[j][0];
            String host[][] = res.get(temp);
            //adding host mac to model
            if(host!=null)
            {
            Resource resNodeHost = RdfOwl.createResource(model, topologyURI + ":" + host[0][0], node);
            model.add(model.createStatement(onosTopology, hasNode, resNodeHost));
            
            Resource resMac = RdfOwl.createResource(model, topologyURI + ":" + host[0][0] + ":macAddress", networkAddress);
            Resource resIP = RdfOwl.createResource(model, topologyURI + ":" + host[0][1] + ":ipAddress", networkAddress);
        
            model.add(model.createStatement(resNodeHost, hasNetworkAddress, resMac));
            model.add(model.createStatement(resNodeHost, hasNetworkAddress, resIP));
            
            model.add(model.createStatement(resMac, type, "mac-addresses"));
            model.add(model.createStatement(resMac, value, host[0][0]));
            model.add(model.createStatement(resIP, type, "ipV4-addresses"));
            model.add(model.createStatement(resIP, value, host[0][1]));
            
            Resource resPortHost = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":port-" + port[j][0], biPort);
            model.add(model.createStatement(resNodeHost, locatedAt, resPortHost));
            }
            }//j<3
            }//device
                }//port
                }//host not null
            }//port not equal to null 
            //flowRule0: in_port
            /*        model.add(model.createStatement(resFlow, flowMatch, resFlowRule0));
                    model.add(model.createStatement(resFlowRule0, type, "IN_PORT"));
                    model.add(model.createStatement(resFlowRule0, value, deviceFlows[j][4]));

                    //flowRule1: ETH_SRC_MAC
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule1));
                    model.add(model.createStatement(resFlowRule1, type, "ETH_SRC_MAC"));
                    model.add(model.createStatement(resFlowRule1, value, deviceFlows[j][6]));

                    //flowRule2: ETH_DST_MAC
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule2));
                    model.add(model.createStatement(resFlowRule2, type, "ETH_DST_MAC"));
                    model.add(model.createStatement(resFlowRule2, value, deviceFlows[j][5]));

                    //flowRule3: ETH_SRC_VLAN
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule3));
                    model.add(model.createStatement(resFlowRule3, type, "ETH_SRC_VLAN"));
                    model.add(model.createStatement(resFlowRule3, value, deviceFlows[j][7]));

                    //flowRule4: ETH_DST_VLAN
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule4));
                    model.add(model.createStatement(resFlowRule4, type, "ETH_DST_VLAN"));
                    model.add(model.createStatement(resFlowRule4, value, deviceFlows[j][8]));

                    //flowAction: OUT_PORT
                    model.add(model.createStatement(resFlow, flowAction, resFlowAction));
                    model.add(model.createStatement(resFlowAction, type, "OUT_PORT"));
                    model.add(model.createStatement(resFlowAction, value, deviceFlows[j][3]));            
                    }  */
                    
            
            //String table[][] = res.getK
        //System.out.println("Test");    
        }
        //}
        
        
        
        //System.out.println("Test");
        //String hello="1";
        return model;
      
    }
        //return null;
    
}
