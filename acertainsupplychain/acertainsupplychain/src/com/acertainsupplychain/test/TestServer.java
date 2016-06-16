package com.acertainsupplychain.test;

import org.eclipse.jetty.server.Server;

import com.acertainsupplychain.CertainOrderManager;
import com.acertainsupplychain.server.ItemSupplierHTTPMessageHandler;
import com.acertainsupplychain.server.OrderManagerHTTPMessageHandler;

/*
 * Starts a server on a seperate thread for testing.
 * 
 */
public class TestServer extends Thread {
    //private final ItemSupplierHTTPMessageHandler handler;
    private final Server server;
    private boolean isHandlerSet = false;
    private CertainOrderManager manager = null;
    
    //
    public TestServer(int port){
        server = new Server(port);
    }
    
    public TestServer(ItemSupplierHTTPMessageHandler handler, int port) {
        server = new Server(port);
        if (handler != null) {
            server.setHandler(handler);
            isHandlerSet = true;
        }
    }
    public TestServer(OrderManagerHTTPMessageHandler handler, int port) {
        server = new Server(port);
        if (handler != null) {
            server.setHandler(handler);
            isHandlerSet = true;
        }
    }
    
    public TestServer(OrderManagerHTTPMessageHandler handler, int port, CertainOrderManager manager) {
        server = new Server(port);
        if (handler != null) {
            server.setHandler(handler);
            isHandlerSet = true;
        }
        this.manager = manager;
    }
    
    /*
     * Do not use this if server is active!
     */
    public void setHandler(ItemSupplierHTTPMessageHandler handler){
        server.setHandler(handler);
        isHandlerSet = true;
    }
    public void setHandler(OrderManagerHTTPMessageHandler handler){
        server.setHandler(handler);
        isHandlerSet = true;
    }
    
    @Override
    public void run() {
        if(isHandlerSet){
            try {
                server.start();
                System.out.println("Server Startet");
                server.join();
                System.out.println("Server closed");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void startServerBlocking(){
        if(isHandlerSet){
            try {
                server.start();
                System.out.println("Server Startet");
                server.join();
                System.out.println("Server closed");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    /*
     * If manager is set, stop it also.
     */
    public void stopServer(){
        try {
            server.stop();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(manager != null){
            manager.stopOrderManager();
        }
    }
}
