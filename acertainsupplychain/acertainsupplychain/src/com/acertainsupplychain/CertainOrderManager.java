package com.acertainsupplychain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.acertainsupplychain.utils.OrderManagerFormatterParameter;
import com.acertainsupplychain.utils.OrderMangerFormatter;
import com.acertainsupplychain.utils.SupplyChainConstants;
import com.acertainsupplychain.utils.SupplyChainUtility;

/**
 * CertainOrderManager implements the OrderManager functionality 
 * which is defined in the assignment and in the OrderManager-interface.
 * 
 * Workflows are submitted and executed asynchronously via RPC to a Supplier
 * having the ItemSupplier-interface. This is handled by a thread running 
 * StepExecutionHandler.
 * 
 * Constructor takes a mapping between item supplier ids and their respective 
 * addresses. Also takes a unique id used for logging (at the item supplier). 
 * Can be null.
 * 
 */
public class CertainOrderManager implements OrderManager {
    private final Integer uniqueManagerId;
    private final Map<Integer, List<StepTuple<OrderStep, StepStatus>>> workflowMap;
    
    private Logger logger; //(CertainOrderManager.class.getName());
    private FileHandler logFile;
    
    private int counter = 1; //initial workflow id
    private Map<Integer, String> supplierServers;
    private final Set<Integer> supplierIds; //TODO needed?
    private CertainOrderManagerStepExecutor stepExecutor;
    private StepExecutionHandler stepExecutorHandler;
    private static final int maxStepExecutorThreads = 10;
    private List<List<Future<StepExecutionResult>>> executionTasks;
    private boolean stopManager = false;

    public CertainOrderManager(Integer uniqueManagerId, 
            Map<Integer, String> supplierServers) 
                    throws SecurityException, IOException {
        
        this.uniqueManagerId = uniqueManagerId;
        //init logger
        String fname = "OrderManager."+ String.valueOf(uniqueManagerId) + ".log";
        logger = Logger.getLogger(fname);
        logFile = new FileHandler(fname);
        logFile.setFormatter(new OrderMangerFormatter());
        logger.addHandler(logFile);
        
        workflowMap = new HashMap<Integer, List<StepTuple<OrderStep, StepStatus>>>();
        this.supplierServers = supplierServers;
        supplierIds = new HashSet<Integer>(supplierServers.keySet());
        stepExecutor = new CertainOrderManagerStepExecutor(maxStepExecutorThreads);
        executionTasks = new ArrayList<List<Future<StepExecutionResult>>>();
        stepExecutorHandler = new StepExecutionHandler();
        stepExecutorHandler.start();
    }
    
    /*
     * Used to kill the stopOrderManager threads nicely.
     */
    public void stopOrderManager(){
        stopManager = true;
        synchronized(executionTasks){
            executionTasks.notify();
        }
        stepExecutor.stop();
    }
    
    /*
     * Update a step of a workflow with a new status.
     */
    private synchronized void updateWorkflow(int workflowId, OrderStep step, 
            StepStatus status){
        List<StepTuple<OrderStep, StepStatus>> workflow = workflowMap.get(workflowId);
        StepTuple<OrderStep, StepStatus> t;
        boolean found = false;
        int i = 0;
        while(!found && i <= workflow.size()){
            t = workflow.get(i);
            
            if(t.step.equals(step)){
                t = new StepTuple<OrderStep, StepStatus>(step, status);
                workflow.set(i, t);
                found = true;
            }
            i++;
        }
    }
    
    /*
     * Add step executioner tasks to already established tasks.
     * These will be handled by StepExecutionHandler.
     */
    private synchronized void addStepExecutionTasks(
            List<Future<StepExecutionResult>> tasks){
        executionTasks.add(tasks);
        synchronized(executionTasks){
            executionTasks.notify();
        }
    }
    
    private void makeStepExecutionTasks(int workflowId,
            List<OrderStep> steps){
        Set<StepExecutionRequest> executionRequests = 
                new HashSet<StepExecutionRequest>();
        StepExecutionRequest request;
        String serverAddress;
        for(OrderStep step : steps){ //make execution requests
            serverAddress = supplierServers.get(step.getSupplierId());
            request = new StepExecutionRequest(serverAddress, uniqueManagerId, workflowId, step);
            executionRequests.add(request);
        }
        try {
            addStepExecutionTasks(stepExecutor.executeStep(executionRequests));
        } catch (Exception e) {
            // System error.
            e.printStackTrace();
        }
    }
    
    /*
     * Returns a list of the current execution tasks in executionTasks and 
     * clears executionTasks.
     */
    private synchronized List<List<Future<StepExecutionResult>>> 
                                                    takeStepExecutionTasks(){
        List<List<Future<StepExecutionResult>>> tasks = 
                new ArrayList<List<Future<StepExecutionResult>>>(executionTasks);
        executionTasks.clear();
        return tasks;
    }
    
    private synchronized boolean isExecutionTasksEmpty(){
        return executionTasks.isEmpty();
    }
    
    /*
     * Meant for updating with results from asynchronous execution of an order 
     * step.
     * Should be forked (threaded) from main. Handles all step executions.
     * When order step has been resolved, update order step status for the 
     * workflow and die.
     */
    class StepExecutionHandler extends Thread {
        private List<List<Future<StepExecutionResult>>> tasks;
        private List<StepExecutionResult> results;

        public StepExecutionHandler(){
            tasks = new ArrayList<List<Future<StepExecutionResult>>>();
            results = new ArrayList<StepExecutionResult>();
        }
        
        @Override
        public void run() {
            int workflow;
            OrderStep step;
            
            
            while(true){
                synchronized(executionTasks){
                    while (isExecutionTasksEmpty() && !stopManager) {
                        try {
                            executionTasks.wait();
                        } catch (InterruptedException e1) {
                            // System error.
                            e1.printStackTrace();
                        }
                    }
                }
                if(stopManager){
                    return;
                }
                tasks = takeStepExecutionTasks();
                //get results
                for(List<Future<StepExecutionResult>> taskList : tasks){
                    for(Future<StepExecutionResult> task : taskList){
                        try {
                            results.add(task.get());
                        } catch (InterruptedException e) {
                            //these are system failures
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
                tasks.clear();
                //update with results
                for(StepExecutionResult result : results){
                    StepStatus status = StepStatus.FAILED;
                    workflow = result.getWorkflowId();
                    step = result.getStep();
                    
                    if(result.isFailStop()){ //resubmit order step
                        List<OrderStep> steps = new ArrayList<OrderStep>();
                        steps.add(step);
                        makeStepExecutionTasks(workflow, steps);
                    } else {
                        if(result.isStepExecutionSuccessful()){
                            status = StepStatus.SUCCESSFUL;
                        }
                        //log results
                        OrderManagerFormatterParameter parameter =
                                new OrderManagerFormatterParameter(
                                        result.getWorkflowId(), 
                                        result.getStep().getSupplierId(),
                                        result.getStep().getItems(),
                                        status);
                        Object[] parameters = {parameter};
                        LogRecord record = new LogRecord(Level.INFO, "Added result");//TODO to be removed?
                        record.setParameters(parameters);
                        synchronized(logger){
                            logger.log(record);
                        }
                        synchronized(logFile){
                            logFile.flush(); //should flush after commit.
                        }
                        updateWorkflow(workflow, step, status);
                    }
                }
                results.clear();
            }
        }
    }
    
    /*
     * Meant for updating with results from asynchronous execution of an order 
     * step.
     * Should be forked (threaded) for each asynchronous call, for each call to 
     * registerOrderWorkflow().
     * When order step has been resolved, update order step status for the 
     * workflow and die.
     */ /*
    private synchronized void executeStepOrder(int workflowId, OrderStep step){
        //TODO USE Future, like MasterCertainBookStore.
    } */

    @Override
    public synchronized int registerOrderWorkflow(List<OrderStep> steps)
            throws OrderProcessingException {
        if (steps == null) {
            throw new OrderProcessingException(SupplyChainConstants.NULL_INPUT);
        }
        //Check workflow for errors
        for (OrderStep step : steps) {
            int supplierId = step.getSupplierId();
            //if (SupplyChainUtility.isInvalidSupplierId(supplierId)){
            if (!supplierIds.contains(supplierId)){ //check if supplier is known
                throw new OrderProcessingException(
                        SupplyChainConstants.SUPPLIERID 
                        + String.valueOf(supplierId) 
                        + SupplyChainConstants.INVALID);
            }
            List<ItemQuantity> items = step.getItems();
            for (ItemQuantity item : items){
                int itemId = item.getItemId();
                int quantity = item.getQuantity();
                if (SupplyChainUtility.isInvalidItemId(itemId)){
                    throw new OrderProcessingException(
                            SupplyChainConstants.ITEM 
                            + String.valueOf(itemId) 
                            + SupplyChainConstants.INVALID);
                } else if (SupplyChainUtility.isInvalidQuantity(quantity)){
                    throw new OrderProcessingException(
                            SupplyChainConstants.QUANTITY 
                            + String.valueOf(quantity) 
                            + SupplyChainConstants.INVALID);
                }
            }
        }
        List <StepTuple<OrderStep, StepStatus>> orderSteps = 
                    new ArrayList<StepTuple<OrderStep, StepStatus>>();
        for (OrderStep step : steps) {
            StepTuple<OrderStep, StepStatus> regStep = 
                    new StepTuple<OrderStep, StepStatus> (step, StepStatus.REGISTERED);
            orderSteps.add(regStep); //keeps order of steps if that matters
        }
        int id = counter++;
        workflowMap.put(id, orderSteps);
        
        //durability; log the action
        OrderManagerFormatterParameter parameter;
        for(OrderStep step : steps){
             parameter = new OrderManagerFormatterParameter(
                     id, 
                     step.getSupplierId(), 
                     step.getItems(), 
                     StepStatus.REGISTERED);
            Object[] parameters = {parameter};
            LogRecord record = new LogRecord(Level.INFO, "Added new workflow");
            record.setParameters(parameters);
            
            synchronized(logger){
                logger.log(record);
            }
            synchronized(logFile){
                logFile.flush(); //should flush after commit.
            }
        }
        
        //Must update orderSteps with orderManager information. Used by logging of itemSupplier
        for(OrderStep step : steps){
            step.setManagerId(uniqueManagerId);
            step.setWorkflowId(id);
        }
        //execute the workflow
        makeStepExecutionTasks(id,steps);
        return id;
    }

    @Override
    public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
            throws InvalidWorkflowException {
        List <StepStatus> stepStatusList = new ArrayList<StepStatus>();
        synchronized(workflowMap){
            if (!workflowMap.containsKey(orderWorkflowId)) {
                throw new InvalidWorkflowException(SupplyChainConstants.WORKFLOW 
                        + String.valueOf(orderWorkflowId) 
                        + SupplyChainConstants.NOT_AVAILABLE);
            }
            List <StepTuple<OrderStep, StepStatus>> orderSteps =  
                    workflowMap.get(orderWorkflowId);
            for (StepTuple<OrderStep, StepStatus> tuple : orderSteps){
                stepStatusList.add(tuple.status);
            }
        }
        return stepStatusList;
    }
    
    /*
     * Just a generic tuple class, used by workflowMap to represent steps.
     */
    class StepTuple<X, Y> { 
        public final X step ; 
        public final Y status; 
        public StepTuple(X step, Y status) { 
          this.step = step; 
          this.status = status; 
        }
    }

}
