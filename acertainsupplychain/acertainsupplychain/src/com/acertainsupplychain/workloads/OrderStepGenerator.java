package com.acertainsupplychain.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.OrderStep;

/**
 * Helper class to generate semi-random OrderSteps.
 */
public class OrderStepGenerator {
    Random r = new Random();
    private ArrayList<Integer> itemIds;
    private int supplierId;
    
    public OrderStepGenerator(Set<Integer> itemIds, int supplierId) {
        this.itemIds = new ArrayList<Integer>(itemIds);
        this.supplierId = supplierId;
    }

    /**
     * Returns a semi-random order step.
     * 
     * @param numItems
     * @return OrderStep
     */
    public OrderStep nextOrderStep(int numItems) {
        List<ItemQuantity> items = new ArrayList<ItemQuantity>();
        Set<Integer> usedIds = new HashSet<Integer>();
        int size = itemIds.size();
        if(numItems > size){
            numItems = size;
        }
        
        for(int i = 0; i < numItems; i++) {
            int id = itemIds.get(r.nextInt(size));
            //ensure unique id each time
            while(usedIds.contains(id)){
                id = itemIds.get(r.nextInt(size));
            }
            usedIds.add(id);
            int quantity = r.nextInt(1000); //just some random, not too high quantity
            ItemQuantity item = new ItemQuantity(id, quantity);
            items.add(item);
            
        }
        return new OrderStep(supplierId, items);
    }
    
    /**
     * Returns a list of order steps.
     * 
     * @param numOrderSteps
     * @param numItems
     * @return list of OrderSteps
     */
    public List<OrderStep> nextListOrderStep(int numOrderSteps, int numItems){
        List<OrderStep> steps = new ArrayList<OrderStep>();
        
        for(int i = 0; i < numOrderSteps; i++){
            steps.add(nextOrderStep(numItems));
        }
        
        return steps;
    }

}
