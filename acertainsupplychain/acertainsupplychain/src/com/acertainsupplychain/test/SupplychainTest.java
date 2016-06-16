package com.acertainsupplychain.test;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.CertainItemSupplier;
import com.acertainsupplychain.CertainOrderManager;
import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.proxy.ItemSupplierHTTPProxy;
import com.acertainsupplychain.proxy.OrderManagerHTTPProxy;
import com.acertainsupplychain.server.ItemSupplierHTTPMessageHandler;
import com.acertainsupplychain.server.OrderManagerHTTPMessageHandler;
import com.acertainsupplychain.utils.SupplierTuple;
import com.acertainsupplychain.utils.SupplyChainUtility;


/*
 * Reads configuration from proxy.properties.test to setup servers.
 * All is done over network.
 * Tests reading configuration and failure propagation.
 */
public class SupplychainTest {
    private static String filePath = 
            "/home/heben2/Dropbox/pcsd/Exam/acertainsupplychain"
            + "/acertainsupplychain/src/com/acertainsupplychain/test"
            + "/proxy.properties.test";
    private static List<TestServer> managerServers;
    private static List<TestServer> supplierServers;
    private static List<ItemSupplierHTTPProxy> supplierProxies;
    private static List<OrderManagerHTTPProxy> managerProxies;
    private static Map<Integer, SupplierTuple<String, Set<Integer>>> suppliersInfo;
    private Boolean isDone = false;
    private boolean fail = true;
    private String failMessage = "";
    
    @BeforeClass
    public static void setUpBeforeClass() {
        managerServers = new ArrayList<TestServer>();
        supplierServers = new ArrayList<TestServer>();
        supplierProxies = new ArrayList<ItemSupplierHTTPProxy>();
        managerProxies = new ArrayList<OrderManagerHTTPProxy>();
        suppliersInfo = initItemSuppliers();
        
        initOrderManagers();
        assertTrue(managerServers.size() > 0);
        assertTrue(supplierServers.size() > 0);
        assertTrue(supplierProxies.size() > 0);
        assertTrue(managerProxies.size() > 0);
        
        System.out.println("manager servers: " + String.valueOf(managerServers.size()));
        System.out.println("supplier servers: " + String.valueOf(supplierServers.size()));
    }
    
    public static void initOrderManagers(){
        Map<Integer, String> managerAddresses = new HashMap<Integer, String>();
        try {
            managerAddresses = 
                    SupplyChainUtility.getOrderManagerAddresses(filePath) ;
        } catch (FileNotFoundException e1) {
            fail("Error when loading file in order manager: "
                    + "File not found.");
        } catch (IOException e1) {
            fail("Error when loading file in order manager: "
                    + "IO exception.");
        }
        OrderManagerHTTPMessageHandler managerHandler;
        OrderManager orderManager;
        TestServer managerServer;
        OrderManagerHTTPProxy managerProxy;
        for (Integer key : managerAddresses.keySet()) {
            String address = managerAddresses.get(key);
            Integer port = SupplyChainUtility.extractPortNumber(address);
            Map<Integer, String> suppliers = new HashMap<Integer, String>();
            try {
                suppliers = 
                        SupplyChainUtility.getItemSupplierAddresses(filePath);
            } catch (FileNotFoundException e1) {
                fail("Error when loading file in order manager: "
                        + "File not found.");
            } catch (IOException e1) {
                fail("Error when loading file in order manager: "
                        + "IO exception.");
            }
            
            try {
                orderManager = new CertainOrderManager(key, suppliers);
                managerHandler = new OrderManagerHTTPMessageHandler(orderManager);
                managerServer = new TestServer(managerHandler, port);
                managerServers.add(managerServer);
                managerServer.start();
                
                managerProxy = new OrderManagerHTTPProxy(address);
                managerProxies.add(managerProxy);
            } catch (SecurityException e1) {
                fail("Error when loading log file in order manager: "
                        + "SecurityException.");
            } catch (IOException e1) {
                fail("Error when loading log file in order manager: "
                        + "IO exception.");
            } catch (Exception e) {
                fail("Error when creating proxy");
            }
        }
    }
    
    public static Map<Integer, SupplierTuple<String, Set<Integer>>> 
                                                        initItemSuppliers(){
      //Read file with item supplier id and item set.
        
        Map<Integer, SupplierTuple<String, Set<Integer>>> suppliers = 
                new HashMap<Integer, SupplierTuple<String, Set<Integer>>>();
        
        try {
            suppliers = SupplyChainUtility.getItemSupplierInfo(filePath);
        } catch (FileNotFoundException e1) {
            fail("Error when loading file: File not found.");
        } catch (IOException e1) {
            fail("Error when loading file: IO exception.");
        }
        CertainItemSupplier itemSupplier;
        ItemSupplierHTTPMessageHandler handler;
        ItemSupplierHTTPProxy supplierProxy;
        TestServer server;
        Set<Integer> itemIds;
        String address;
        Integer port;
        for(Integer id : suppliers.keySet()){
            itemIds = suppliers.get(id).itemIds;
            address = suppliers.get(id).address;
            port = SupplyChainUtility.extractPortNumber(address);
            try {
                itemSupplier = new CertainItemSupplier(id, itemIds);
                handler = new ItemSupplierHTTPMessageHandler(itemSupplier);
                server = new TestServer(handler, port);
                supplierServers.add(server);
                server.start();
                supplierProxy = new ItemSupplierHTTPProxy(address);
                supplierProxies.add(supplierProxy);
            } catch (InvalidItemException e) {
                fail("Error on item input: Invalid item Id.");
            } catch (Exception e) {
                fail("Error when creating proxy");
            }
        }
        return suppliers;
    }
    
    
    /*
     * Interface for the assertions given to threadTest.
     */
    private interface TestAssert{
        /**
         * Contains the assertion
         */
        public void asserts();
    }
    /*
     * Starts threads of the given list of runnables, runs them 
     */
    private void threadTest(Runnable[] ts, TestAssert ta, int k) {
        ArrayList<Thread> tList = new ArrayList<Thread>();
        if(k < 0){
            return;
        }
        for(int i=0; i<k;i++){
            for(Runnable t : ts){
                Thread tmpT = new Thread(t);
                tmpT.start();
                tList.add(tmpT);
            }
            for(Thread t : tList){
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            }
            ta.asserts();
        }
    }
    
    
    /**
     * Here we tests both CertainOrderManager and CertainItemSupplier with a big
     * setup. Test that failure does not propagate.
     * 
     * Setup: 
     *  - 2 order managers
     *  - 3 item suppliers
     *  - 3 unique items per item supplier.
     * 
     * Let two threads use an order manager each.
     * Kill one manager (by closing it) and thread.
     * 
     * Assert that all other components live.
     * 
     * Kill one item supplier.
     * 
     * Assert that all other components live and that the status for an order 
     * step for the killed supplier stays as registered.
     * 
     */
    @Test
    public void testSupplyChain() {
        final int iterations = 10;
        
        List<OrderStep> steps = new ArrayList<OrderStep>();
        List<ItemQuantity> items;
        OrderStep step;
        ItemQuantity item;
        int quantity =  5;
        
        for(Integer key : suppliersInfo.keySet()){
            items = new ArrayList<ItemQuantity>();
            SupplierTuple<String, Set<Integer>> t = suppliersInfo.get(key);
            Set<Integer> itemIds = t.itemIds;
            Integer itemId = null;
            //just get one itemId
            for(Integer id : itemIds){
                itemId = id;
                break;
            }
            item = new ItemQuantity(itemId, quantity);
            items.add(item);
            step = new OrderStep(key, items);
            steps.add(step);
        }
        
        //Adds the same workflow to a manger, hitting all item suppliers. Repeats a number of iteratoins.
        class C1 implements Runnable{
            private OrderManagerHTTPProxy manager;
            private List<OrderStep> steps;
            private List<Integer> workflowIds = new ArrayList<Integer>();
            
            public C1(List<OrderStep> steps){
                this.steps = steps;
                if(managerProxies.size() > 0){
                    manager = managerProxies.get(0);
                } else {
                    fail("managerProxies not loaded correctly");
                }
            }

            @Override
            public void run() {
                //do some workflows
                for(int i = 0; i < iterations; i++){
                    try {
                        workflowIds.add(manager.registerOrderWorkflow(steps));
                    } catch (OrderProcessingException e) {
                        fail = true;
                        return;
                    }
                }
                try {
                    Thread.sleep(2000); //wait 2 seconds
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                    fail = true;
                    return;
                }
                //other manager should be dead now.
                synchronized(isDone){
                    failMessage = "Other thread should be done now.";
                    fail = !isDone;
                }
                //See that we are still alive and item suppliers responds
                for(int i = 0; i < iterations; i++){
                    try {
                        workflowIds.add(manager.registerOrderWorkflow(steps));
                    } catch (OrderProcessingException e) {
                        fail = true;
                        return;
                    }
                }
                
                //assert itemsuppliers are alive before and after a managers 
                //failure by seeing all items are successful.
                List<StepStatus> stepStatuses;
                
                for(Integer workflowId : workflowIds){
                    int i = 0;
                    boolean isExecuted = false;
                    while(i++ < 50 && !isExecuted) {//wait a maximum of 5 seconds
                        try {
                            Thread.sleep(100); //wait .1 seconds per try to get a response
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                            fail = true;
                            return;
                        }
                        try {
                            stepStatuses = manager.getOrderWorkflowStatus(workflowId);
                            boolean allChanged = true;
                            for(StepStatus status : stepStatuses){
                                //test status has changed
                                if(status == StepStatus.REGISTERED){
                                    allChanged = false;
                                }
                            }
                            if(allChanged){
                                isExecuted = true;
                            }
                        } catch (InvalidWorkflowException e) {
                            failMessage = "Invalid workflow: this should not happen";
                            fail = true;
                            return;
                        }
                    }
                    if(!isExecuted){
                        failMessage = "StepStatus never changed";
                        fail = true;
                        return;
                    }
                }
                
                //TODO Jetty does not throw exception when server is down.
                //Jetty simply prints stacktrace... Thank you Jetty!
                //http://stackoverflow.com/questions/21160180/jetty-ioexception-never-thrown
                //kill first itemsupplier
                //supplierServers.get(0).stopServer();
                //remove it for take down
                //supplierServers.remove(0);
                
                workflowIds = new ArrayList<Integer>();
                //do some more workflows
                for(int i = 0; i < iterations; i++){
                    try {
                        workflowIds.add(manager.registerOrderWorkflow(steps));
                    } catch (OrderProcessingException e) {
                        fail = true;
                        return;
                    }
                }
                
                //Test that all but first supplier responds
                
                for(Integer workflowId : workflowIds){
                    int i = 0;
                    boolean isExecuted = false;
                    while(i++ < 50 && !isExecuted) {//wait a maximum of 5 seconds
                        try {
                            Thread.sleep(100); //wait .1 seconds per try to get a response
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                            fail = true;
                            return;
                        }
                        try {
                            stepStatuses = manager.getOrderWorkflowStatus(workflowId);
                            int numberChanged = 0;
                            for(StepStatus status : stepStatuses){
                                //test all statuses but one has changed
                                if(status != StepStatus.REGISTERED){
                                    numberChanged++;
                                }
                            }
                            if(stepStatuses.size()-1 == numberChanged){
                                isExecuted = true;
                            }
                            
                        } catch (InvalidWorkflowException e) {
                            failMessage = "Invalid workflow: this should not happen";
                            fail = true;
                            return;
                        }
                    }
                    failMessage = "One status should not change";
                    fail = !isExecuted;
                    return;
                }
                
                fail = false;
            }
            
        }
        
        //Waits 3 seconds then closes last manager down.
        class C2 implements Runnable{
            private TestServer manager;
            private int index;
            
            public C2(){
                index = managerServers.size()-1;
                manager = managerServers.get(index);
            }
            
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                    fail();
                }
                
                manager.stopServer();
                managerServers.remove(index);
                synchronized(isDone){
                    isDone = true;
                }
            }
            
        }
        //Assert item suppliers still alive 
        class Assertion implements TestAssert {
            @Override
            public void asserts() {
                //TODO Assert item suppliers are alive and updated correctly.
                if(fail){
                    fail(failMessage);
                }
                
            }
        }
        
        Assertion a = new Assertion();
        Runnable[] ts = {new C1(steps), new C2()};
        threadTest(ts, a, 1);
        
    }
    
    
    @AfterClass
    public static void tearDownAfterClass() {
        for(OrderManagerHTTPProxy proxy : managerProxies){
            proxy.stop();
        }
        for(ItemSupplierHTTPProxy proxy : supplierProxies){
            proxy.stop();
        }
        for(TestServer server : managerServers){
            server.stopServer();
        }
        for(TestServer server : supplierServers){
            server.stopServer();
        }
    }
}
