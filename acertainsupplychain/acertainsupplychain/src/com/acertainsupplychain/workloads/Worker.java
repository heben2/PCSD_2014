/**
 * 
 */
package com.acertainsupplychain.workloads;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private OrderStepGenerator orderStepGenerator = null;
    private OrderManager globalClient;
    private ItemSupplier localClient;
    private int numTotalGlobalClientRuns = 0;

    public Worker(WorkloadConfiguration config) {
        logger.addHandler(new ConsoleHandler());
        logger.info("Worker initialized");
        configuration = config;
        orderStepGenerator = configuration.getOrderStepGenerator();
        globalClient = configuration.getManagerProxy();
        localClient = configuration.getSupplierProxy();
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     * 
     * Updates the counts of total runs and successful runs for customer
     * interaction
     * 
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
        try {
            if (chooseInteraction < configuration
                    .getPercentGlobalClient()) {
                numTotalGlobalClientRuns++;
                runGlobalClient();
            } else {
                runLocalClient();
            } 
        } catch (Exception ex) {
            return false; //unsuccessful interaction.
        }
        return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
        int count = 1;
        long startTimeInNanoSecs = 0;
        long endTimeInNanoSecs = 0;
        int successfulInteractions = 0;
        long timeForRunsInNanoSecs = 0;

        Random rand = new Random();
        float chooseInteraction;

        logger.info("Warming up");
        // Perform the warmup runs
        while (count++ <= configuration.getWarmUpRuns()) {
            chooseInteraction = rand.nextFloat() * 100f;
            runInteraction(chooseInteraction);
        }

        count = 1;
        numTotalGlobalClientRuns = 0;
        
        logger.info("Starting benchmark" );
        // Perform the actual runs
        startTimeInNanoSecs = System.nanoTime();
        while (count++ <= configuration.getNumActualRuns()) {
            chooseInteraction = rand.nextFloat() * 100f;
            if (runInteraction(chooseInteraction)) {
                successfulInteractions++;
            }
        }
        endTimeInNanoSecs = System.nanoTime();
        timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
        logger.info("Benchmark complete");
        
        //close down manager server and supplier and manager proxies.
        configuration.stopManager();
        configuration.stopSupplier();
        
        System.out.println("successfulinteractions:");
        System.out.println(successfulInteractions);
        System.out.println("total global interactions:");
        System.out.println(numTotalGlobalClientRuns);
        System.out.println("total interactions:");
        System.out.println(configuration.getNumActualRuns());
        
        return new WorkerRunResult(successfulInteractions,
                timeForRunsInNanoSecs, configuration.getNumActualRuns(),
                numTotalGlobalClientRuns);
    }
    
    
    /**
     * Runs global interaction
     * 
     * @throws OrderProcessingException 
     */
    private void runGlobalClient() throws OrderProcessingException {
        List<OrderStep> steps = 
                orderStepGenerator
                .nextListOrderStep(configuration.getNumOrderStep(), 
                        configuration.getNumberOfItemsPerOrderStepGlobal());
        
        int workflowId = globalClient.registerOrderWorkflow(steps);
        
        List<StepStatus> statusList;
        int i = 0;
        boolean isExecuted = false;
        while(i++ < configuration.getNumWaits() && !isExecuted) {
            try {
                Thread.sleep(100); //wait .1 seconds per try to get a response
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new OrderProcessingException(e);
            }
            statusList = globalClient.getOrderWorkflowStatus(workflowId);
            if(statusList == null || statusList.size() != 1){
                throw new OrderProcessingException();
            }
            if(statusList.get(0) != StepStatus.REGISTERED){ //hardcoded
                isExecuted = true;
            }
        }
        
        if(!isExecuted){
            throw new OrderProcessingException(); //unsuccessful
        }
    }

    /**
     * 
     * Runs local interaction
     * 
     * @throws OrderProcessingException 
     */
    private void runLocalClient() throws OrderProcessingException {
        OrderStep step = orderStepGenerator.nextOrderStep(configuration
                .getNumberOfItemsPerOrderStepLocal());
        localClient.executeStep(step);
    }

    private static Logger logger = Logger.getLogger(Worker.class.getName());
}
