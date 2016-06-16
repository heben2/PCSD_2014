/**
 * 
 */
package com.acertainsupplychain.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.acertainsupplychain.CertainItemSupplier;
import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.utils.SupplierTuple;
import com.acertainsupplychain.utils.SupplyChainUtility;


/**
 * Starts all given item suppliers (instances) given in a file.
 * args[0] will be the filepath to the configuration file of the item suppliers.
 * Expects a file with this format: 
 * suppliers=[supplierId] [supplierAddress] [itemId,itemId,..]
 * Example:
 * suppliers=1 localhost:8082 1,2,3,4,5,6;2 localhost:8083 2,3,4
 */
public class CertainItemSupplierHTTPServer {
    /**
     * @param args
     */
    public static void main(String[] args) {
        //Read file with item supplier id and item set.
        String filePath = args[0];
        
        Map<Integer, SupplierTuple<String, Set<Integer>>> suppliers = 
                new HashMap<Integer, SupplierTuple<String, Set<Integer>>>();
        
        try {
            suppliers = SupplyChainUtility.getItemSupplierInfo(filePath);
        } catch (FileNotFoundException e1) {
            System.out.println("Error when loading file: File not found.");
            System.exit(0);
        } catch (IOException e1) {
            System.out.println("Error when loading file: IO exception.");
            System.exit(0);
        }
        ItemSupplier itemSupplier;
        Set<Integer> items;
        String address;
        Integer port;
        for(Integer id : suppliers.keySet()){
            items = suppliers.get(id).itemIds;
            address = suppliers.get(id).address;
            port = SupplyChainUtility.extractPortNumber(address);
            try {
                itemSupplier = new CertainItemSupplier(id, items);
                ItemSupplierHTTPMessageHandler handler = new ItemSupplierHTTPMessageHandler(itemSupplier);
                SupplyChainHTTPServerUtility.createServer(port, handler);
            } catch (InvalidItemException e) {
                System.out.println("Error on item input: Invalid item Id.");
                System.exit(0);
            } catch (SecurityException e) {
                System.out.println("Error when creating log file in item "
                        + "supplier: SecurityException.");
                System.exit(0);
            } catch (IOException e) {
                System.out.println("Error when creating log file in item "
                        + "supplier: IO exception.");
                System.exit(0);
            }
        }
        
        
    }
}
