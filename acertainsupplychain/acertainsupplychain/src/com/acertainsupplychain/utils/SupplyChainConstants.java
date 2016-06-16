/**
 * 
 */
package com.acertainsupplychain.utils;

/**
 * SupplyChainConstants declares the constants used in the SupplyChain by
 * both servers and clients. Used to define error messages and to interpret 
 * input files.
 * 
 */
public final class SupplyChainConstants {

	// Constants used when creating URLs	
	public static final String XMLSTRINGLEN_PARAM = "len";
	
	// Used as error code when converting numbers to integer
	public static final int INVALID_PARAMS = -1;
	
	// Constants used when creating exception messages
	public static final String INVALID = " is invalid";
	public static final String NOT_AVAILABLE = " is not available";
	public static final String WORKFLOW = "The workflow: ";
	public static final String STEP = "The step: ";
	public static final String SUPPLIERID = "The supplierId: ";
	public static final String ITEM = "The item: ";
	public static final String QUANTITY = "The item quantity: ";
	public static final String NULL_INPUT = "null input parameters";
	
    public static final String KEY_SUPPLIER = "suppliers";
    public static final String KEY_MANAGER = "managers";
    public static final String SPLIT_SUPPLIER_REGEX = ";";
    public static final String SPLIT_INTERNAL_REGEX = " ";
    public static final String SPLIT_INTERNAL_SUPPLIER_ITEM_REGEX = ",";
}
