package com.acertainsupplychain.proxy;

import java.util.List;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utils.CommunicationUtility;
import com.acertainsupplychain.utils.MessageTag;

public class OrderManagerHTTPProxy implements OrderManager {
    protected HttpClient client;
    protected String serverAddress;

    /**
     * Initialize the client object
     */
    public OrderManagerHTTPProxy(String serverAddress) throws Exception {
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    
    @Override
    public int registerOrderWorkflow(List<OrderStep> steps)
            throws OrderProcessingException {
        int result;
        ContentExchange exchange = new ContentExchange();
        String urlString;
        urlString = serverAddress + "/" + MessageTag.REGISTERORDERWORKFLOW;

        String stepXmlString = CommunicationUtility
                .serializeObjectToXMLString(steps);
        exchange.setMethod("POST");
        exchange.setURL(urlString);
        Buffer requestContent = new ByteArrayBuffer(stepXmlString);
        exchange.setRequestContent(requestContent);
        
        try {
            result = (int) CommunicationUtility.SendAndRecvId(this.client, exchange);
        } catch (Exception e) {
            throw new OrderProcessingException(e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
            throws InvalidWorkflowException {
        List<StepStatus> result;
        
        ContentExchange exchange = new ContentExchange();
        String urlString = serverAddress + "/" + MessageTag.GETORDERWORKFLOWSTATUS;

        String listISBNsxmlString = CommunicationUtility
                .serializeObjectToXMLString(orderWorkflowId);
        exchange.setMethod("POST");
        exchange.setURL(urlString);
        Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);
        exchange.setRequestContent(requestContent);

        try {
            result = (List<StepStatus>) CommunicationUtility.SendAndRecv(this.client, exchange);
        } catch (Exception e) {
            throw new InvalidWorkflowException(e);
        }
        return result;
    }
}
