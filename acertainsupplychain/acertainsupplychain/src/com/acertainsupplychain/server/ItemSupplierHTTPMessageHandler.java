/**
 *
 */
package com.acertainsupplychain.server;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utils.CommunicationResponse;
import com.acertainsupplychain.utils.CommunicationUtility;
import com.acertainsupplychain.utils.MessageTag;

/**
 * ItemSupplierHTTPMessageHandler implements the message handler class which is
 * invoked to handle messages received by the SupplyChainHTTPServerUtility. It
 * decodes the HTTP message and invokes the ItemSupplier server API
 */
public class ItemSupplierHTTPMessageHandler extends AbstractHandler {
    private final ItemSupplier instance;
    public ItemSupplierHTTPMessageHandler(ItemSupplier instance){
        this.instance = instance;
    }
    
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        MessageTag messageTag;
        String requestURI;
        
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        requestURI = request.getRequestURI();

        // Need to do request multi-plexing
        if (!CommunicationUtility.isEmpty(requestURI)
                && requestURI.toLowerCase().startsWith("/stock")) {
            messageTag = CommunicationUtility.convertURItoMessageTag(requestURI
                    .substring(6));
        } else {
            messageTag = CommunicationUtility.convertURItoMessageTag(requestURI);
        }
        
        // the RequestURI before the switch
        if (messageTag == null) {
            System.out.println("Unknown message tag");
        } else {
            String xml;
            CommunicationResponse CommunicationResponse;
            String listStepsxmlString;
            switch (messageTag) {
            case EXECUTESTEP:
                xml = CommunicationUtility
                        .extractPOSTDataFromRequest(request);
                
                OrderStep step = (OrderStep) CommunicationUtility
                        .deserializeXMLStringToObject(xml);
                
                CommunicationResponse = new CommunicationResponse();
                try {
                    instance.executeStep(step);
                } catch (OrderProcessingException ex) {
                    CommunicationResponse.setException(ex);
                }
                
                listStepsxmlString = CommunicationUtility
                        .serializeObjectToXMLString(CommunicationResponse);
                response.getWriter().println(listStepsxmlString);
                break;
            case GETORDERSPERITEM:
                xml = CommunicationUtility
                    .extractPOSTDataFromRequest(request);
                
                @SuppressWarnings("unchecked")
                Set<Integer> itemIds = (Set<Integer>) CommunicationUtility
                        .deserializeXMLStringToObject(xml);
                
                CommunicationResponse = new CommunicationResponse();
                try {
                    List<ItemQuantity> items = instance.getOrdersPerItem(itemIds);
                    CommunicationResponse.setList(items);
                } catch (InvalidItemException ex) {
                    CommunicationResponse.setException(ex);
                }
                
                listStepsxmlString = CommunicationUtility
                        .serializeObjectToXMLString(CommunicationResponse);
                response.getWriter().println(listStepsxmlString);
                break;
            default:
                System.out.println("Unhandled message tag");
                break;
            }
        }
        // Mark the request as handled so that the HTTP response can be sent
        baseRequest.setHandled(true);
    }
}
