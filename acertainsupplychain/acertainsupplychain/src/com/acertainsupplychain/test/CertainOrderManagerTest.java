package com.acertainsupplychain.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.CertainItemSupplier;
import com.acertainsupplychain.CertainOrderManager;
import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.proxy.ItemSupplierHTTPProxy;
import com.acertainsupplychain.proxy.OrderManagerHTTPProxy;
import com.acertainsupplychain.server.ItemSupplierHTTPMessageHandler;
import com.acertainsupplychain.server.OrderManagerHTTPMessageHandler;



public class CertainOrderManagerTest {
    private static boolean local = false; //Test local order manager or over (local) network
    //OrderManager
    private static OrderManager manager;
    private static int managerId = 1;
    private static OrderManagerHTTPProxy managerProxy;
    private static OrderManagerHTTPMessageHandler managerHandler;
    private static TestServer managerServer;
    private static int managerPort = 8082;
    private static String serverAddressManager = 
            "http://localhost:" + String.valueOf(managerPort);
    
    //ItemSupplier
    private static ItemSupplier supplier;
    private static ItemSupplierHTTPProxy supplierProxy;
    private static ItemSupplierHTTPMessageHandler supplierHandler;
    private static TestServer supplierServer;
    private static int supplierPort = 8081;
    private static String serverAddressSupplier = 
            "http://localhost:" + String.valueOf(supplierPort);
    private static Set<Integer> itemIds;
    private static int supplierId;
            
    @BeforeClass
    public static void setUpBeforeClass() {
        itemIds = new HashSet<Integer>();
        itemIds.add(1);
        itemIds.add(2);
        itemIds.add(3);
        itemIds.add(4);
        supplierId = 1;
        try {
            ItemSupplier itemSupplier;
            try {
                itemSupplier = new CertainItemSupplier(supplierId, itemIds);
                supplierHandler = new ItemSupplierHTTPMessageHandler(itemSupplier);
                supplierServer = new TestServer(supplierHandler, supplierPort);
                supplierServer.start();
                supplierProxy = new ItemSupplierHTTPProxy(serverAddressSupplier);
                supplier = supplierProxy;
            } catch (InvalidItemException e) {
                fail("Error SupplierSever: Invalid item");
            }
            HashMap<Integer, String> itemSuppliers = 
                    new HashMap<Integer, String>();
            itemSuppliers.put(supplierId, serverAddressSupplier);
            
            
            if(local){
                manager = new CertainOrderManager(managerId, itemSuppliers);
            } else {
                OrderManager orderManager = 
                        new CertainOrderManager(managerId, itemSuppliers);
                managerHandler = 
                        new OrderManagerHTTPMessageHandler(orderManager);
                managerServer = new TestServer(managerHandler, managerPort);
                managerServer.start();
                managerProxy = new OrderManagerHTTPProxy(serverAddressManager);
                manager = managerProxy;
            }
        } catch (Exception e) {
            fail("Initialization failed.");
        }
    }
    
    /**
     * Here we tests CertainOrderManager.registerOrderWorkflow functionality
     * 
     * 1. Fails when registering invalid item suppliers.
     * 
     * 2. Fails when registering invalid item ids.
     * 
     * 3. Fails when registering invalid quantities.
     * 
     */
    @Test
    public void testRegisterOrderWorkflowErrors() {
        List<OrderStep> steps = new ArrayList<OrderStep>();
        //1
        OrderStep step = new OrderStep(-1, null);
        steps.add(step);
        boolean error = false;
        try {
            manager.registerOrderWorkflow(steps);
        } catch (OrderProcessingException e) {
            error = true;
        }
        assertTrue("registorOrderWorkflow should not accept invalid item "
                + "supplier id", error);
        
        //2
        steps = new ArrayList<OrderStep>();
        List<ItemQuantity> items = new ArrayList<ItemQuantity>();
        ItemQuantity item = new ItemQuantity(-1, 0);
        items.add(item);
        step = new OrderStep(1, items);
        steps.add(step);
        error = false;
        try {
            manager.registerOrderWorkflow(steps);
        } catch (OrderProcessingException e) {
            error = true;
        }
        assertTrue("registorOrderWorkflow should not accept invalid item ids", 
                error);
        
        //3
        steps = new ArrayList<OrderStep>();
        items = new ArrayList<ItemQuantity>();
        item = new ItemQuantity(1, -1);
        items.add(item);
        step = new OrderStep(1, items);
        steps.add(step);
        error = false;
        try {
            manager.registerOrderWorkflow(steps);
        } catch (OrderProcessingException e) {
            error = true;
        }
        assertTrue("registorOrderWorkflow should not accept invalid item "
                + "quantity", error);
    }
    
    /**
     * Here we tests CertainOrderManager.registerOrderWorkflow and 
     * .getOrderWorkflowStatus functionality
     * 
     * 1. Test that a valid workflow can be added and is added.
     * 
     * 2. Test step of workflow is eventually be successful (shows asynchronous 
     * behavior, as it does not block on registerOrderWorkflow).
     * Try 200 times and sleep .1 second between each try and note if multiple 
     * tries where necessary before change in status of step.
     * 
     * 3. Test item supplier (item quantity) is actually updated.
     * 
     * 4. Test invalid (as in item supplier does not have that item) step of 
     * workflow is eventually set to failed (shows asynchronous 
     * behavior, as it does not block on registerOrderWorkflow).
     * Try 200 times and sleep .1 second between each try and note if multiple 
     * tries where necessary before change in status of step.
     * 
     * 5. Test logging is done.//TODO
     */
    @Test
    public void testRegisterOrderWorkflow() {
        List<OrderStep> steps = new ArrayList<OrderStep>();
        List<ItemQuantity> items = new ArrayList<ItemQuantity>();
        //1
        OrderStep step;
        ItemQuantity item;
        
        steps = new ArrayList<OrderStep>();
        items = new ArrayList<ItemQuantity>();
        item = new ItemQuantity(1, 1);
        items.add(item);
        step = new OrderStep(1, items);
        steps.add(step);
        
        List<StepStatus> stepStatus = null;
        int workflowId = 0;
        try {
            workflowId = manager.registerOrderWorkflow(steps);
        } catch (Exception e) {
            fail();
        }
        try {
            stepStatus = manager.getOrderWorkflowStatus(workflowId);
        } catch (Exception e) {
            fail();
        }
        assertNotNull(stepStatus);
        
        //2
        int i = 0;
        boolean isExecuted = false;
        
        while(i++ < 200 && !isExecuted) { //only try that many times.
            try {
                Thread.sleep(100); //wait .1 seconds per try to get a response
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                fail();
            }
            try {
                stepStatus = manager.getOrderWorkflowStatus(workflowId);
                assertEquals(stepStatus.size(), 1);
                
                if(stepStatus.get(0) != StepStatus.REGISTERED){
                    isExecuted = true;
                    assertEquals(StepStatus.SUCCESSFUL, stepStatus.get(0));
                }
            } catch (InvalidWorkflowException e) {
                fail("Invalid workflow: this should not happen");
            }
        }
        assertTrue("Execution should not possibly finish in under 100 miliseconds", i > 1);
        if(!isExecuted){
            fail("StepStatus never changed");
        }
        
        //3
        try {
            items = supplier.getOrdersPerItem(itemIds);
        } catch (InvalidItemException e) {
            fail("Invalid item: This should not happen.");
        }
        //Item quantity should be the same as added; 0+1=1.
        assertTrue(items.contains(item));
        
        //4
        //TODO
        int notPresentItemId = 100;
        steps = new ArrayList<OrderStep>();
        items = new ArrayList<ItemQuantity>();
        item = new ItemQuantity(notPresentItemId, 1);
        items.add(item);
        step = new OrderStep(1, items);
        steps.add(step);
        int newWorkflowId = 0;
        try {
            newWorkflowId = manager.registerOrderWorkflow(steps);
            assertNotEquals(newWorkflowId, workflowId);
        } catch (Exception e) {
            fail();
        }
        try {
            stepStatus = manager.getOrderWorkflowStatus(newWorkflowId);
        } catch (Exception e) {
            fail();
        }
        i = 0;
        isExecuted = false;
        while(i++ < 200 && !isExecuted) {
            try {
                Thread.sleep(100); //wait .1 seconds per try to get a response
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                fail();
            }
            try {
                stepStatus = manager.getOrderWorkflowStatus(newWorkflowId);
                assertEquals(stepStatus.size(), 1);
                
                if(stepStatus.get(0) != StepStatus.REGISTERED){
                    isExecuted = true;
                    assertEquals(StepStatus.FAILED, stepStatus.get(0));
                }
            } catch (InvalidWorkflowException e) {
                fail("Invalid workflow: this should not happen");
            }
        }
        assertTrue("Execution should not possibly finish in under 100 miliseconds", i > 1);
        if(!isExecuted){
            fail("StepStatus never changed");
        }
        
        
        //5
        //TODO
    }
    
    
    @AfterClass
    public static void tearDownAfterClass() {
        if(!local){
            supplierProxy.stop();
            managerProxy.stop();
            try {
                supplierServer.stopServer();
                managerServer.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
