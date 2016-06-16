package com.acertainsupplychain.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utils.CommunicationResponse;
import com.acertainsupplychain.utils.CommunicationUtility;
import com.acertainsupplychain.utils.MessageTag;

/**
 * OrderManagerHTTPMessageHandler implements the message handler class which is
 * invoked to handle messages received by the SupplyChainHTTPServerUtility. It
 * decodes the HTTP message and invokes the OrderManager server API
 */
public class OrderManagerHTTPMessageHandler extends AbstractHandler {
    private final OrderManager instance;
    public OrderManagerHTTPMessageHandler(OrderManager instance){
        this.instance = instance;
    }
    
	@SuppressWarnings("unchecked")
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
			case REGISTERORDERWORKFLOW:
				xml = CommunicationUtility
						.extractPOSTDataFromRequest(request);

				List<OrderStep> steps = (List<OrderStep>) CommunicationUtility
						.deserializeXMLStringToObject(xml);

				CommunicationResponse = new CommunicationResponse();
				try {
				    int workflowId = instance.registerOrderWorkflow(steps);
				    CommunicationResponse.setId(workflowId);
				} catch (OrderProcessingException ex) {
					CommunicationResponse.setException(ex);
				}
				
				listStepsxmlString = CommunicationUtility
						.serializeObjectToXMLString(CommunicationResponse);
				response.getWriter().println(listStepsxmlString);
				break;
			case GETORDERWORKFLOWSTATUS:
                xml = CommunicationUtility
                    .extractPOSTDataFromRequest(request);
                int workflowId = (Integer) CommunicationUtility
                        .deserializeXMLStringToObject(xml);
        
                CommunicationResponse = new CommunicationResponse();
                try {
                    List<StepStatus> stepStatus = instance.getOrderWorkflowStatus(workflowId);
                    CommunicationResponse.setList(stepStatus);
                } catch (InvalidWorkflowException ex) {
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
