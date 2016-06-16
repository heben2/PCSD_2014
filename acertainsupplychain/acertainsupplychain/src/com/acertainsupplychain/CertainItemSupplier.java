package com.acertainsupplychain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.acertainsupplychain.utils.ItemSupplierFormatter;
import com.acertainsupplychain.utils.ItemSupplierFormatterParameter;
import com.acertainsupplychain.utils.SupplyChainConstants;
import com.acertainsupplychain.utils.SupplyChainUtility;

/*
 * CertainItemSupplier implementing the ItemSupplier-interface.
 * A supplier contains some items and their ordered amount.
 * This amount is accumulated through executions of steps.
 * 
 * All actions, both read and writes, must be atomic.
 */
public class CertainItemSupplier implements ItemSupplier {
    private final int id;
    private final HashMap<Integer, Integer> items;
    
    private Logger logger;
    private FileHandler logFile;
    
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
    //lock is fair to avoid starvation
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();
    
    public CertainItemSupplier(int supplierId, Set<Integer> itemIds) 
            throws InvalidItemException, SecurityException, IOException {
        id = supplierId;
        
        //init logger
        String fname = "ItemSupplier."+ String.valueOf(supplierId) + ".log";
        logger = Logger.getLogger(fname);
        logFile = new FileHandler(fname);
        logFile.setFormatter(new ItemSupplierFormatter());
        logger.addHandler(logFile);
        
        items = new HashMap<Integer, Integer>();
        for(Integer itemId : itemIds){
            if(itemId < 0){
                throw new InvalidItemException();
            }
            items.put(itemId, 0);
        }
    }
    
    /*
     * Validation is also meant to ensure all-or-nothing atomicity 
     * (assumed we cannot crash when actually updating).
     * When actually updating, before-or-after atomicity is ensured by write 
     * lock all the way through.
     * The operation is assumed to succeed by caller if no exception is thrown.
     * @see com.acertainsupplychain.ItemSupplier#executeStep(com.acertainsupplychain.OrderStep)
     */
    @Override
    public void executeStep(OrderStep step) throws OrderProcessingException {
        //Validate step to make sure all-or-nothing atomicity (assumed we cannot crash when actually updating).
        int supplierId = step.getSupplierId();
        if (supplierId != id){
            throw new OrderProcessingException(SupplyChainConstants.SUPPLIERID + String.valueOf(supplierId) + SupplyChainConstants.INVALID);
        }
        List<ItemQuantity> orderItems = step.getItems();
        for (ItemQuantity item : orderItems){ //only reading ids; no lock needed
            int itemId = item.getItemId();
            int quantity = item.getQuantity();
            if (!items.containsKey(itemId)){
                throw new OrderProcessingException(SupplyChainConstants.ITEM + String.valueOf(itemId) + SupplyChainConstants.INVALID);
            } else if (SupplyChainUtility.isInvalidQuantity(quantity)){
                throw new OrderProcessingException(SupplyChainConstants.QUANTITY + String.valueOf(quantity) + SupplyChainConstants.INVALID);
            }
        }
        
        //durability; log action
        Integer managerId = step.getManagerId();
        Integer workflowId = step.getWorkflowId();
        ItemSupplierFormatterParameter parameter = 
                new ItemSupplierFormatterParameter(
                        managerId,
                        workflowId,
                        step.getItems());
        Object[] parameters = {parameter};
        LogRecord record = new LogRecord(Level.INFO, "Added new order step");
        record.setParameters(parameters);
        
        synchronized(logger){
            logger.log(record);
        }
        synchronized(logFile){
            logFile.flush(); //should flush after commit.
        }
        
        //Atomically (before-after) update quantities
        w.lock();
        for (ItemQuantity item : orderItems){
            int itemId = item.getItemId();
            int quantity = item.getQuantity();
            int oldQuantity = items.get(itemId);
            items.put(itemId, oldQuantity+quantity);
        }
        w.unlock();
    }

    /*
     * Takes a read lock immediately as can abort if itemId is not found 
     * without precautions. Aborts if item not found.
     * @see com.acertainsupplychain.ItemSupplier#getOrdersPerItem(java.util.Set)
     */
    @Override
    public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
            throws InvalidItemException {
        List<ItemQuantity> orderItems = new ArrayList<ItemQuantity>();
        Integer quantity;
        r.lock();
        for (Integer itemId : itemIds){
            quantity = items.get(itemId);
            if(quantity != null){
                orderItems.add(new ItemQuantity(itemId, quantity)); //should be immutable
            } else {
                r.unlock();
                throw new InvalidItemException(SupplyChainConstants.ITEM 
                        + String.valueOf(itemId) + SupplyChainConstants.INVALID);
            }
        }
        r.unlock();
        return orderItems;
    }

}
