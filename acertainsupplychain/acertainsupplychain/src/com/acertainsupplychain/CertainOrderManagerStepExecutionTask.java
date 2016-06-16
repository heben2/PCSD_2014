package com.acertainsupplychain;

import java.net.ConnectException;
import java.util.concurrent.Callable;

import com.acertainsupplychain.proxy.StepExecutionHTTPProxy;

/*
 * The step execution task for a given step execution by an order manager.
 */
public class CertainOrderManagerStepExecutionTask implements
                                Callable<StepExecutionResult> {
    StepExecutionHTTPProxy proxy;
    int workflowId;
    OrderStep step;
    
    public CertainOrderManagerStepExecutionTask(StepExecutionRequest request) 
            throws Exception {
        proxy = new StepExecutionHTTPProxy(request.getServerAddres());
        workflowId = request.getWorkflowId();
        step = request.getOrderStep();
    }
    
    @Override
    public StepExecutionResult call() throws Exception {
        StepExecutionResult result = new StepExecutionResult(workflowId, step, true);
        try{
            proxy.executeStep(step);
        } catch(OrderProcessingException e){
            result.setStepExecutionSuccessful(false);
        } catch(ConnectException e){
            result.setStepExecutionSuccessful(false);
            result.setFailStop(true);
        } catch(Exception e){ //fail-stop of item supplier
            result.setStepExecutionSuccessful(false);
            result.setFailStop(true);
        }
        //kill the proxy, otherwise we get a thread leak.
        proxy.stop();
        return result;
    }

}
