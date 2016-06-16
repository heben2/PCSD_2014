package com.acertainsupplychain.workloads;

import java.util.HashMap;
import java.util.Set;

import com.acertainsupplychain.CertainOrderManager;
import com.acertainsupplychain.proxy.ItemSupplierHTTPProxy;
import com.acertainsupplychain.proxy.OrderManagerHTTPProxy;
import com.acertainsupplychain.server.OrderManagerHTTPMessageHandler;
import com.acertainsupplychain.test.TestServer;
import com.acertainsupplychain.utils.SupplyChainUtility;

/**
 * 
 * WorkloadConfiguration represents the configuration parameters to be used by
 * Workers class for running the workloads
 * 
 */
public class WorkloadConfiguration {
    private final String supplierServerAddress;
    private final int supplierId = 1;
    private final HashMap<Integer, String> supplierMap;
    private Set<Integer> itemIds;
    private TestServer managerServer;
    private OrderManagerHTTPProxy managerProxy;
    private ItemSupplierHTTPProxy supplierProxy;
    private int warmUpRuns = 20;
    private int numActualRuns = 200;//100;
    private int numberOfItemsPerOrderStepLocal = 2;
    private int numberOfItemsPerOrderStepGlobal = 10;
    private int numOrderStep = 1;
    private int numWaits = 100; //the number of times an order manager should wait 0.1 sec for results
    private float percentGlobalClient = 70f;
    private float percentLocalClient = 30f;
    private OrderStepGenerator orderStepGenerator = null;
    private final int managerPort;

    public WorkloadConfiguration(String supplierServerAddress, int managerPort, 
            Set<Integer> itemIds)
            throws Exception {
        this.supplierServerAddress = supplierServerAddress;
        this.managerPort = managerPort;
        this.itemIds = itemIds;
        supplierMap = new HashMap<Integer, String>();
        supplierMap.put(supplierId, supplierServerAddress);
        // Create a new one so that it is not shared
        orderStepGenerator = new OrderStepGenerator(itemIds, supplierId);
        initOrderManager();
        initItemSupplierProxy();
        
    }
    
    /*
     * Get the item supplier address.
     */
    public String getSupplierAddres(){
        return supplierServerAddress;
    }
    
    /*
     * Returns new item supplier proxy or null.
     */
    public void initItemSupplierProxy(){
        try {
            supplierProxy = new ItemSupplierHTTPProxy(supplierServerAddress);
        } catch (Exception e) {
            // Should not happen.
            e.printStackTrace();
        }
    }
    
    /*
     * Starts a new TestServer with a new order manager.
     * All stated manager servers can be stopped by calling 
     * closeManagerServers().
     * Returns proxy to new order manager or null.
     */
    private void initOrderManager(){
        String OrderManagerServerAddress = getNewFreeAddress();
        int port = SupplyChainUtility.
                extractPortNumber(OrderManagerServerAddress);
        ;
        try {
            CertainOrderManager certainOrderManager = 
                    new CertainOrderManager(port, supplierMap);
            OrderManagerHTTPMessageHandler handler = 
                    new OrderManagerHTTPMessageHandler(certainOrderManager);
            managerServer = new TestServer(handler, port, certainOrderManager);
            managerServer.start();
            managerProxy = new OrderManagerHTTPProxy(OrderManagerServerAddress);
        } catch (Exception e) {
            //should not happen.
            e.printStackTrace();
        }
    }
    
    private String getNewFreeAddress(){
        String address = "http://localhost:" + String.valueOf(managerPort);
        return address;
    }
    
    public void stopManager(){
        managerServer.stopServer();
        managerProxy.stop();
    }
    
    public void stopSupplier(){
        supplierProxy.stop();
    }

    public float getPercentGlobalClient() {
        return percentGlobalClient;
    }

    public void setPercentGlobalClient(float percentGlobalClient) {
        this.percentGlobalClient = percentGlobalClient;
    }

    public float getPercentLocalClient() {
        return percentLocalClient;
    }

    public void setPercentLocalClient(float percentLocalClient) {
        this.percentLocalClient = percentLocalClient;
    }

    public int getWarmUpRuns() {
        return warmUpRuns;
    }

    public void setWarmUpRuns(int warmUpRuns) {
        this.warmUpRuns = warmUpRuns;
    }

    public int getNumActualRuns() {
        return numActualRuns;
    }

    public void setNumActualRuns(int numActualRuns) {
        this.numActualRuns = numActualRuns;
    }
    
    public OrderStepGenerator getOrderStepGenerator() {
        return orderStepGenerator;
    }

    public void setOrderStepGenerator(OrderStepGenerator orderStepGenerator) {
        this.orderStepGenerator = orderStepGenerator;
    }
    
    public void setItemIds(Set<Integer> itemIds){
        this.itemIds = itemIds;
    }
    
    public Set<Integer> getItemIds(){
        return itemIds;
    }
    
    public int getNumberOfItemsPerOrderStepLocal(){
        return numberOfItemsPerOrderStepLocal;
    }
    
    public int getNumberOfItemsPerOrderStepGlobal(){
        return numberOfItemsPerOrderStepGlobal;
    }
    
    public int getNumOrderStep(){
        return numOrderStep;
    }
    
    public int getNumWaits(){
        return numWaits;
    }
    
    public OrderManagerHTTPProxy getManagerProxy(){
        return managerProxy;
    }
    
    public ItemSupplierHTTPProxy getSupplierProxy(){
        return supplierProxy;
    }
}
