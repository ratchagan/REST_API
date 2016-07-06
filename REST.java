/**
 *
 * @author ranjitha
 */

@Stateless
public class OdlRESTDriver implements IHandleDriverSystemCall {
    Logger logger = Logger.getLogger(OdlRESTDriver.class.getName());
    String fakeMap="";
    //String requests="";
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)  
    
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId()); // refresh
        String access_key_id = driverInstance.getProperty("odl_access_key_id");
        String secret_access_key = driverInstance.getProperty("odl_secret_access_key");
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String mappingId = driverInstance.getProperty("mappingId");

        String model = driverInstance.getHeadVersionItem().getModelRef().getTtlModel();
        
        String modelAdd = aDelta.getModelAddition().getTtlModel();
        String modelReduc = aDelta.getModelReduction().getTtlModel();
                
        OdlPush push = new OdlPush();
        
        String requests = null;
        try {
            requests = push.pushPropagate(access_key_id, secret_access_key, mappingId, model, modelAdd, modelReduc, topologyURI, subsystemBaseUrl);
            
        } catch (Exception ex) {
            Logger.getLogger(OdlRESTDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
            throw (new EJBException(ex));
        }
        
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        driverInstance.putProperty(requestId, requests);
        DriverInstancePersistenceManager.merge(driverInstance);
        Logger.getLogger(OdlRESTDriver.class.getName()).log(Level.INFO, "ODL REST driver delta models succesfully propagated");
       }

        
    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }
        String access_key_id = driverInstance.getProperty("odl_access_key_id");
        String secret_access_key = driverInstance.getProperty("odl_secret_access_key");
        //Searches for topologyURI in Driver Instance
        String topologyURI = driverInstance.getProperty("topologyUri");
        
        
        String mappingId = driverInstance.getProperty("mappingId");
        
        //Searches for subsystemBaseUrl in Driver Instance
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null || access_key_id == null || secret_access_key ==null || topologyURI == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);
        
        OdlPush push = new OdlPush();
        
        try {
            push.pushCommit( access_key_id,  secret_access_key,requests, mappingId, topologyURI,  subsystemBaseUrl, aDelta);
        } catch (Exception ex) {
            Logger.getLogger(OdlRESTDriver.class.getName()).log(Level.SEVERE, null, ex);
            throw(new EJBException(ex));
        }

        driverInstance.getProperties().remove(requestId);
        //DriverInstancePersistenceManager.merge(driverInstance);
        Logger.getLogger(OdlRESTDriver.class.getName()).log(Level.INFO, "ODL driver delta models succesfully commited");
        fakeMap=push.getFakeFlowId();
        driverInstance.putProperty("mappingId", fakeMap);
        DriverInstancePersistenceManager.merge(driverInstance);
        
        return new AsyncResult<String>("SUCCESS");
    }
    
    @Override
    @Asynchronous
    @SuppressWarnings("empty-statement")
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
        try {
            String access_key_id = driverInstance.getProperty("odl_access_key_id");
            String secret_access_key = driverInstance.getProperty("odl_secret_access_key");
            //Searches for topologyURI in Driver Instance
            String topologyURI = driverInstance.getProperty("topologyUri");
            String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");

            String srrgFile = driverInstance.getProperty("srrg");
        
            
            String mappingId = driverInstance.getProperty("mappingId");
            
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }

        // Creates an Ontology Model for ONOS Server
        OntModel ontModel = OdlModelBuilder.createOntology(topologyURI,subsystemBaseUrl, srrgFile, mappingId, access_key_id, secret_access_key);
                       
        if (driverInstance.getHeadVersionItem() == null || !driverInstance.getHeadVersionItem().getModelRef().getOntModel().isIsomorphicWith(ontModel)) {
                DriverModel dm = new DriverModel();
                dm.setCommitted(true);
                dm.setOntModel(ontModel);
                ModelPersistenceManager.save(dm);

                VersionItem vi = new VersionItem();
                vi.setModelRef(dm);
                vi.setReferenceUUID(UUID.randomUUID().toString());
                vi.setDriverInstance(driverInstance);
                VersionItemPersistenceManager.save(vi);
                driverInstance.setHeadVersionItem(vi);
            } 
           
        } catch (IOException e) {
            throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, e.getMessage()));
        } catch (Exception ex) {
            Logger.getLogger(OdlRESTDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
            throw(new EJBException(ex));
        }
        
        return new AsyncResult<>("SUCCESS");
    }
    
      private int executeHttpMethod(String access_key_id,String secret_access_key,URL url, HttpURLConnection conn, String method, String body) throws IOException {
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
        //logger.log(Level.INFO, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        //logger.log(Level.INFO, "Response Code : {0}", responseCode);

        return responseCode;
    }

  
}
