package com.acertainsupplychain.utils;

import java.util.List;

import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.OrderManager.StepStatus;

public class OrderManagerFormatterParameter {
    private final int workflowId;
    private final int supplierId;
    private final List<ItemQuantity> items;
    private final StepStatus status;
    
    public OrderManagerFormatterParameter(int workflowId, int supplierId, 
            List<ItemQuantity> items, StepStatus status){
        this.workflowId = workflowId;
        this.supplierId = supplierId;
        this.items = items;
        this.status = status;
    }
    
    public int getWorkflowId(){
        return workflowId;
    }
    
    public int getSupplierId(){
        return supplierId;
    }
    
    public List<ItemQuantity> getItems(){
        return items;
    }
    
    public StepStatus getStatus(){
        return status;
    }
    
}
