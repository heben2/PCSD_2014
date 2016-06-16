package com.acertainsupplychain.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.CertainItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.proxy.ItemSupplierHTTPProxy;
import com.acertainsupplychain.server.ItemSupplierHTTPMessageHandler;


public class CertainItemSupplierTest {
    private static boolean local = false; //Test local or over (local) network
    
    private static ItemSupplier supplier;
    private static ItemSupplierHTTPProxy supplierProxy;
    private static ItemSupplierHTTPMessageHandler handler;
    private static TestServer server;
    private static Set<Integer> itemIds;
    private static int supplierId;
    private static int port = 8081;
    private static String serverAddress = 
            "http://localhost:" + String.valueOf(port);
            
    @BeforeClass
    public static void setUpBeforeClass() {
        itemIds = new HashSet<Integer>();
        itemIds.add(1);
        itemIds.add(2);
        itemIds.add(3);
        itemIds.add(4);
        supplierId = 1;
        try {
            if(local){
                supplier = new CertainItemSupplier(supplierId, itemIds);
            } else {
                ItemSupplier itemSupplier;
                try {
                    itemSupplier = new CertainItemSupplier(supplierId, itemIds);
                    handler = new ItemSupplierHTTPMessageHandler(itemSupplier);
                    server = new TestServer(handler, port);
                    server.start();
                    supplierProxy = new ItemSupplierHTTPProxy(serverAddress);
                    supplier = supplierProxy;
                } catch (InvalidItemException e) {
                    System.out.println("Error server: Invalid item");
                    fail();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * Here we tests CertainItemSupplier.getOrdersPerItem functionality
     * 
     * 1. Tests items are all initialized.
     * 
     * 2. getOrdersPerItem() returns those items given.
     * 
     * 3. Fails when given item not of supplier.
     * 
     */
    @Test
    public void testGetOrdersPerItem() {
        //1
        List<ItemQuantity> items = new ArrayList<ItemQuantity>();
        try {
            items = supplier.getOrdersPerItem(itemIds);
        } catch (InvalidItemException e) {
            fail();
        }
        assertTrue("List should be as long as added items",
                items.size() == itemIds.size());
        
        //2
        Set<Integer> someItems = new HashSet<Integer>();
        someItems.add(2);
        someItems.add(3);
        try {
            items = supplier.getOrdersPerItem(someItems);
        } catch (InvalidItemException e) {
            fail();
        }
        assertTrue("Gets more items than asked for", items.size() == 2);
        
        for(ItemQuantity item : items){
            int id = item.getItemId();
            assertTrue("Items returned not right items", someItems.contains(id));
        }
        
        //3
        boolean isInvalidItem = false;
        Set<Integer> invalidItems = new HashSet<Integer>();
        invalidItems.add(99);
        try {
            items = supplier.getOrdersPerItem(invalidItems);
        } catch (InvalidItemException e) {
            isInvalidItem = true;
        }
        assertTrue(isInvalidItem);
    }
    
    /**
     * Here we test CertainItemSupplier.executeStep functionality
     * 
     * 1. Tests items are initialized to 0 quantity (again).
     * 
     * 2. Fails if incorrect supplier id, negative item id or negative quantity 
     * is given.
     * 
     * 3. Updates quantity for only given items and updates correctly.
     *    Note this touches itemId 2.
     * 
     * 4. Test logging is done.//TODO
     * 
     */
    @Test
    public void testExecuteStep() {
        //1
        List<ItemQuantity> items = new ArrayList<ItemQuantity>();
        try {
            items = supplier.getOrdersPerItem(itemIds);
        } catch (InvalidItemException e) {
            fail();
        }
        for(ItemQuantity item : items){
            int quantity = item.getQuantity();
            assertTrue("Items should have quantity 0", quantity == 0);
        }
        
        //2
        List<ItemQuantity> itemQuantitiesError = new ArrayList<ItemQuantity>();
        int itemIdError1 = 2;
        int quantityError1 = 5;
        ItemQuantity itemQuantityError1 = new ItemQuantity(itemIdError1, quantityError1);
        int itemIdError2 = 4;
        int quantityError2 = 11;
        ItemQuantity itemQuantityError2 = new ItemQuantity(itemIdError2, quantityError2);
        itemQuantitiesError.add(itemQuantityError1);
        itemQuantitiesError.add(itemQuantityError2);
        OrderStep stepError1 = new OrderStep(supplierId+1, itemQuantitiesError);
        boolean isError = false;
        try {
            supplier.executeStep(stepError1);
        } catch (OrderProcessingException e) {
            isError = true;
        }
        assertTrue("Should fail when supplierId is wrong", isError);
        
        List<ItemQuantity> itemQuantitiesError3 = new ArrayList<ItemQuantity>();
        int itemIdError3 = -4;
        int quantityError3 = 11;
        ItemQuantity itemQuantityError3 = new ItemQuantity(itemIdError3, quantityError3);
        itemQuantitiesError3.add(itemQuantityError3);
        OrderStep stepError3 = new OrderStep(supplierId, itemQuantitiesError3);
        isError = false;
        try {
            supplier.executeStep(stepError3);
        } catch (OrderProcessingException e) {
            isError = true;
        }
        assertTrue("Should fail when item id is negative", isError);
        
        List<ItemQuantity> itemQuantitiesError4 = new ArrayList<ItemQuantity>();
        int itemIdError4 = 4;
        int quantityError4 = -11;
        ItemQuantity itemQuantityError4 = new ItemQuantity(itemIdError4, quantityError4);
        itemQuantitiesError4.add(itemQuantityError4);
        OrderStep stepError4 = new OrderStep(supplierId, itemQuantitiesError4);
        isError = false;
        try {
            supplier.executeStep(stepError4);
        } catch (OrderProcessingException e) {
            isError = true;
        }
        assertTrue("Should fail when quantity is negative", isError);
        
        //3
        List<ItemQuantity> itemQuantities = new ArrayList<ItemQuantity>();
        int itemId1 = 2;
        int quantity1 = 5;
        ItemQuantity itemQuantity1 = new ItemQuantity(itemId1, quantity1);
        itemQuantities.add(itemQuantity1);
        OrderStep step = new OrderStep(supplierId, itemQuantities);
        try {
            supplier.executeStep(step);
        } catch (OrderProcessingException e) {
            fail();
        }
        Set<Integer> updatedItems = new HashSet<Integer>();
        updatedItems.add(itemId1);
        List<ItemQuantity> itemsReturned = new ArrayList<ItemQuantity>();
        try {
            itemsReturned = supplier.getOrdersPerItem(updatedItems);
        } catch (InvalidItemException e) {
            fail();
        }
        for(ItemQuantity item : itemsReturned){
            assertTrue("Item id not found", item.getItemId() == itemId1);
            assertTrue("Item quantity wrong", item.getQuantity() == quantity1);
        }
        
    }
    
    
    /**
     * Here we test CertainItemSupplier.executeStep and .getOrdersPerItem 
     * atomicity.
     * 
     * We create multiple threads all updating quantity and retrieving on the 
     * same item.
     * Quantity should add up in the end to prove atomicity.
     * 
     * 5 threads: Each updates 100 times with quantity 1.
     * 2 threads: retrieves items 250 times. Do not use this to anything but to 
     * interfere the above 5 threads.
     * 
     * Uses the untouched itemId = 3.
     */
    @Test
    public void testExecuteStepAtomicity() {
        final int numThreads = 5;
        final int numUpdates = 100;
        final int numRetrieves = 250;
        final int itemId = 3;
        final int quantity = 1;
        
        class C1 implements Runnable{
            @Override
            public void run() {
                ItemQuantity item = new ItemQuantity(itemId, quantity);
                List<ItemQuantity> items = new ArrayList<ItemQuantity>();
                items.add(item);
                OrderStep step = new OrderStep(supplierId, items);
                for(int i=0; i<numUpdates; i++){
                    try {
                        supplier.executeStep(step);
                    } catch (OrderProcessingException e) {
                        e.printStackTrace();
                        fail("Invalid order: should not happen.");
                    }
                }
            }
        }
        
        class C2 implements Runnable{
            @Override
            public void run() {
                List<ItemQuantity> items;
                Set<Integer> itemIds = new HashSet<Integer>();
                itemIds.add(itemId);
                for(int i=0; i<numRetrieves; i++){
                    try {
                        items = supplier.getOrdersPerItem(itemIds);
                        assertEquals(items.size(),1);
                    } catch (InvalidItemException e) {
                        e.printStackTrace();
                        fail("Invalid order: should not happen.");
                    }
                }
            }
        }
        
        class Assertion implements TestAssert {
            @Override
            public void asserts() {
                int totalQuantity = numUpdates*quantity*numThreads;
                Set<Integer> updatedItems = new HashSet<Integer>();
                updatedItems.add(itemId);
                List<ItemQuantity> itemsReturned = new ArrayList<ItemQuantity>();
                try {
                    itemsReturned = supplier.getOrdersPerItem(updatedItems);
                } catch (InvalidItemException e) {
                    fail("Invalid itemId to retrieve");
                }
                
                assertEquals(itemsReturned.size(), 1);
                assertEquals(itemId, itemsReturned.get(0).getItemId());
                assertEquals(totalQuantity, itemsReturned.get(0).getQuantity());
            }
        }
        
        Assertion a = new Assertion();
        Runnable[] ts = {new C1(), new C1(), new C1(), new C1(), new C1(), new C2(), new C2()};
        threadTest(ts, a, 1);
    }
    
    
    @AfterClass
    public static void tearDownAfterClass() {
        if(!local){
            supplierProxy.stop();
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
