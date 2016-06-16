package com.acertainsupplychain.utils;

import java.util.List;

import com.acertainsupplychain.ItemQuantity;

public class ItemSupplierFormatterParameter {
    private final Integer orderManagerId;
    private final Integer workflowId;
    private final List<ItemQuantity> items;
    
    public ItemSupplierFormatterParameter(Integer orderManagerId, Integer workflowId, 
            List<ItemQuantity> items){
        this.orderManagerId = orderManagerId;
        this.workflowId = workflowId;
        this.items = items;
    }
    
    public Integer getOrderManagerId(){
        return orderManagerId;
    }
    
    public Integer getWorkflowId(){
        return workflowId;
    }
    
    public List<ItemQuantity> getItems(){
        return items;
    }
    
}
