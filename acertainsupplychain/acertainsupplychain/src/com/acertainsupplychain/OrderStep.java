package com.acertainsupplychain;

import java.util.List;

/**
 * An OrderStep instance contains a quantity ordered against specific items, all
 * managed by a specific item supplier.
 * 
 * Added equals method and the possibility to add an order manager id and a 
 * workflow id, used for logging by the item supplier, if set. Can be null.
 */
public final class OrderStep {
    /**
     * The order manager id of the order step, if any.
     */
    private Integer managerId = null;
    
    /**
     * The workflow id of the order step, if any.
     */
    private Integer workflowId = null;

	/**
	 * The ID of the item supplier that manages the items.
	 */
	private final int supplierId;

	/**
	 * The list of items ordered and their quantities.
	 */
	private final List<ItemQuantity> items;

	/**
	 * Constructs an OrderStep instance with given supplier, item, and quantity.
	 */
	public OrderStep(int supplierId, List<ItemQuantity> items) {
		this.supplierId = supplierId;
		this.items = items;
	}

	/**
	 * @return the supplierId
	 */
	public int getSupplierId() {
		return supplierId;
	}

	/**
	 * @return the items
	 */
	public List<ItemQuantity> getItems() {
		return items;
	}
	
	
	public void setManagerId(Integer id){
	    managerId = id;
	}
	
	public Integer getManagerId(){
	    return managerId;
	}
	
	public void setWorkflowId(Integer id){
        workflowId = id;
    }
	
	public Integer getWorkflowId(){
	    return workflowId;
	}
	
	
	/*
	 * equals only on supplierId and items.
	 */
	public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if ( (this.getSupplierId() == ((OrderStep) obj).getSupplierId())
                && (this.getItems().equals(((OrderStep) obj).getItems()))) {
            return true;
        }
        return false;
    }

}
