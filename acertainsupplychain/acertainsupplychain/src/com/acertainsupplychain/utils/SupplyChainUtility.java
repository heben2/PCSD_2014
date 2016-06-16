package com.acertainsupplychain.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/*
 * SupplyChainUtility implements utility methods used by SupplyChain servers and
 * clients.
 */
public class SupplyChainUtility {
    
    public static boolean isInvalidQuantity(int quantity) {
        return (quantity < 0);
    }
    public static boolean isInvalidItemId(int itemId) {
        return (itemId < 1);
    }
    public static boolean isInvalidSupplierId(int supplierId) {
        return (supplierId < 1);
    }
    
    /*
     * Takes an address to a file containing order manager info.
     * Returns a map of order manager ids to addresses.
     *  Expects a file by this format: 
     * managers=[managerId] [managerAddress];[managerId] [managerAddress]...
     * suppliers=[supplierId] [supplierAddress] [itemId,itemId,..(IGNORED)];[supplierId]..
     * 
     * Example:
     * managers=1 localhost\:8080;2 localhost\:8081
     * suppliers=1 localhost\:8082 1,2,3,4,5,6;2 localhost\:8083 2,3,4
     */
    public static Map<Integer, String> getOrderManagerAddresses(String filePath) 
            throws FileNotFoundException, IOException{
        Map<Integer, String> managerServers = new HashMap<Integer, String>();
        Properties props = new Properties();
        props.load(new FileInputStream(filePath));
        
        String managers = props
                .getProperty(SupplyChainConstants.KEY_MANAGER);
        for (String managerInfo : managers
                .split(SupplyChainConstants.SPLIT_SUPPLIER_REGEX)) {
            
            String[] supplierInfoList = managerInfo
                    .split(SupplyChainConstants.SPLIT_INTERNAL_REGEX);
            Integer id = Integer.valueOf(supplierInfoList[0]);
            String managerAddress = supplierInfoList[1];
            
            if (!managerAddress.toLowerCase().startsWith("http://")) {
                managerAddress = new String("http://" + managerAddress);
            }
            if (managerAddress.endsWith("/")) {
                managerAddress = managerAddress.substring(0, managerAddress.length()-1);
                //managerAddress = new String(managerAddress + "/");
            }
            managerServers.put(id, managerAddress);
        }
        
        return managerServers;
    }
    
    /*
     * Takes an address to a file containing item supplier info.
     * Returns a map of supplier id to supplier address.
     *  Expects a file by this format: 
     * managers=[managerId] [managerAddress];[managerId] [managerAddress]...
     * suppliers=[supplierId] [supplierAddress] [itemId,itemId,..(IGNORED)];[supplierId]..
     * 
     * Example:
     * managers=1 localhost\:8080;2 localhost\:8081
     * suppliers=1 localhost\:8082 1,2,3,4,5,6;2 localhost\:8083 2,3,4
     */
    public static Map<Integer, String> getItemSupplierAddresses(String filePath) 
            throws FileNotFoundException, IOException{
        Properties props = new Properties();
        Map<Integer, String> supplierServers = new HashMap<Integer, String>();

        props.load(new FileInputStream(filePath));

        String supplierAddresses = props
                .getProperty(SupplyChainConstants.KEY_SUPPLIER);
        for (String supplierInfo : supplierAddresses
                .split(SupplyChainConstants.SPLIT_SUPPLIER_REGEX)) {
            String[] supplierInfoList = supplierInfo
                    .split(SupplyChainConstants.SPLIT_INTERNAL_REGEX);
            Integer id = Integer.valueOf(supplierInfoList[0]);
            String supplierAddress = supplierInfoList[1];
            
            if (!supplierAddress.toLowerCase().startsWith("http://")) {
                supplierAddress = new String("http://" + supplierAddress);
            }
            if (supplierAddress.endsWith("/")) {
                supplierAddress = supplierAddress.substring(0, supplierAddress.length()-1);
                //supplierAddress = new String(supplierAddress + "/");
            }
            
            supplierServers.put(id, supplierAddress);
        }
        
        return supplierServers;
    }
    
    /*
     * Takes an address to a file containing item supplier info.
     * Returns a map of supplier id to a tuple with supplier address and 
     * supplier item ids.
     *  Expects a file by this format: 
     * managers=[managerId] [managerAddress];[managerId] [managerAddress]...
     * suppliers=[supplierId] [supplierAddress] [itemId,itemId,..];[supplierId]..
     * 
     * Example:
     * managers=1 localhost\:8080;2 localhost\:8081
     * suppliers=1 localhost\:8082 1,2,3,4,5,6;2 localhost\:8083 2,3,4
     */
    public static Map<Integer, SupplierTuple<String, Set<Integer>>> 
                            getItemSupplierInfo(String filePath) 
                                    throws FileNotFoundException, IOException {
        Map<Integer, SupplierTuple<String, Set<Integer>>> supplierServers = 
                new HashMap<Integer, SupplierTuple<String, Set<Integer>>>();
        Properties props = new Properties();
        props.load(new FileInputStream(filePath));

        String suppliers = props
                .getProperty(SupplyChainConstants.KEY_SUPPLIER);
        
        for (String supplierInfo : suppliers
                .split(SupplyChainConstants.SPLIT_SUPPLIER_REGEX)) {
            
            String[] supplierInfoList = supplierInfo
                    .split(SupplyChainConstants.SPLIT_INTERNAL_REGEX);
            
            Integer id = Integer.valueOf(supplierInfoList[0]);
            String supplierAddress = supplierInfoList[1];
            String itemsString = supplierInfoList[2];
            
            if (!supplierAddress.toLowerCase().startsWith("http://")) {
                supplierAddress = new String("http://" + supplierAddress);
            }
            if (supplierAddress.endsWith("/")) {
                supplierAddress = supplierAddress.substring(0, supplierAddress.length()-1);
                //supplierAddress = new String(supplierAddress + "/");
            }
            
            Set<Integer> items = new HashSet<Integer>();
            //Convert itemIds
            for(String item : itemsString.split(SupplyChainConstants
                    .SPLIT_INTERNAL_SUPPLIER_ITEM_REGEX)){
                items.add(Integer.valueOf(item));
            }
            SupplierTuple<String, Set<Integer>> info = 
                    new SupplierTuple<String, Set<Integer>>(supplierAddress, 
                            items);
            supplierServers.put(id, info);
        }
        return supplierServers;
    }
    
    public static Integer extractPortNumber(String address){
        Integer port;
        
        if (!address.endsWith("/")) {
            port = Integer.valueOf(address.split("\\:")[2]);
        } else {
            port = Integer.valueOf(
                    address.split("\\:")[2].replaceFirst("/", ""));
        }
        return port;
    }
}

