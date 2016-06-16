package com.acertainsupplychain.proxy;

import java.util.List;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utils.CommunicationUtility;
import com.acertainsupplychain.utils.MessageTag;


/*
 * Used by the step executor.
 * Looks almost the same as ItemSupplierHTTPProxy, but can throw other types of
 * errors, which is needed to identify fail-stop of item supplier.
 */
public class StepExecutionHTTPProxy {
    protected HttpClient client;
    protected String serverAddress;

    /**
     * Initialize the client object
     * @throws Exception 
     */
    public StepExecutionHTTPProxy(String serverAddress) throws Exception {
        setServerAddress(serverAddress);
        client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setMaxConnectionsPerAddress(ProxyConstants.CLIENT_MAX_CONNECTION_ADDRESS);// max
                                                                                    // concurrent
                                                                                    // connections
                                                                                    // to
                                                                                    // every
                                                                                    // address
        client.setThreadPool(new QueuedThreadPool(
                ProxyConstants.CLIENT_MAX_THREADSPOOL_THREADS));         // max
                                                                    // threads
        client.setTimeout(ProxyConstants.CLIENT_MAX_TIMEOUT_MILLISECS);  // seconds
                                                                    // timeout;
                                                                    // if
                                                                    // no
                                                                    // server
                                                                    // reply,
                                                                    // the
                                                                    // request
                                                                    // expires
        client.start();
    }
    
    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    
    
    public void executeStep(OrderStep step) throws OrderProcessingException, Exception {
        ContentExchange exchange = new ContentExchange();
        String urlString;
        urlString = serverAddress + "/" + MessageTag.EXECUTESTEP;

        String stepXmlString = CommunicationUtility
                .serializeObjectToXMLString(step);
        exchange.setMethod("POST");
        exchange.setURL(urlString);
        Buffer requestContent = new ByteArrayBuffer(stepXmlString);
        exchange.setRequestContent(requestContent);
        
        CommunicationUtility.SendAndRecv(this.client, exchange);
        
    }

    @SuppressWarnings("unchecked")
    public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
            throws InvalidItemException {
        List<ItemQuantity> result;
        ContentExchange exchange = new ContentExchange();
        String urlString = serverAddress + "/" + MessageTag.GETORDERSPERITEM;
        
        String listISBNsxmlString = CommunicationUtility
                .serializeObjectToXMLString(itemIds);
        exchange.setMethod("POST");
        exchange.setURL(urlString);
        Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);
        exchange.setRequestContent(requestContent);
        try {
            result = (List<ItemQuantity>) CommunicationUtility.SendAndRecv(this.client, exchange);
        } catch (Exception e) {
            throw new InvalidItemException(e);
        }
        return result;
    }
}
