/**
 * 
 */
package com.acertainsupplychain.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.acertainsupplychain.CertainOrderManager;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.utils.SupplyChainUtility;


/**
 * Starts all given order managers (instances) given in a file.
 * They will (only) know all item managers given in the file.
 * args[0] will be the filepath to the configuration file of the item managers.
 * Expects a file by this format: 
 * managers=[managerAddress];[managerAddress]...
 * suppliers=[managerId] [managerAddress] [itemId,itemId,..(IGNORED)];[managerId]..
 * 
 * Example:
 * managers=1 localhost:8080;2 localhost:8081
 * suppliers=1 localhost:8082 1,2,3,4,5,6;2 localhost:8083 2,3,4
 */
public class CertainOrderManagerHTTPServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    //Read file with item manager id and item set.
	    String filePath = args[0];
	    Map<Integer, String> managerAddresses = new HashMap<Integer, String>();
	    try {
            managerAddresses = 
                    SupplyChainUtility.getOrderManagerAddresses(filePath) ;
	    } catch (FileNotFoundException e1) {
            System.out.println("Error when loading file in order manager: "
                    + "File not found.");
            System.exit(0);
        } catch (IOException e1) {
            System.out.println("Error when loading file in order manager: "
                    + "IO exception.");
            System.exit(0);
        }
        
        for (Integer key : managerAddresses.keySet()) {
            String address = managerAddresses.get(key);
            Integer port = SupplyChainUtility.extractPortNumber(address);
            Map<Integer, String> suppliers = new HashMap<Integer, String>();
            try {
                suppliers = 
                        SupplyChainUtility.getItemSupplierAddresses(filePath);
            } catch (FileNotFoundException e1) {
                System.out.println("Error when loading file in order manager: "
                        + "File not found.");
                System.exit(0);
            } catch (IOException e1) {
                System.out.println("Error when loading file in order manager: "
                        + "IO exception.");
                System.exit(0);
            }
            
            OrderManager manager;
            try {
                manager = new CertainOrderManager(key, suppliers);
                OrderManagerHTTPMessageHandler handler = 
                        new OrderManagerHTTPMessageHandler(manager);
                SupplyChainHTTPServerUtility.createServer(port, handler);
            } catch (SecurityException e) {
                System.out.println("Error when creating log file in order "
                        + "manager: SecurityException.");
                System.exit(0);
            } catch (IOException e) {
                System.out.println("Error when creating log file in order "
                        + "manager: IO exception.");
                System.exit(0);
            }
        }
	}
}
