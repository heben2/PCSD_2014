package com.acertainsupplychain.utils;

/*
 * A generic tuple class, used by getItemSupplierInfo.
 */
public class SupplierTuple <X, Y> {
    public final X address ; 
    public final Y itemIds; 
    
    public SupplierTuple(X address, Y itemIds) { 
      this.address = address; 
      this.itemIds = itemIds; 
    }
}
