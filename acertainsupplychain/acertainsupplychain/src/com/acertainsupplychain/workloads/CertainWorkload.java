/**
 * 
 */
package com.acertainsupplychain.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.acertainsupplychain.CertainItemSupplier;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.server.ItemSupplierHTTPMessageHandler;
import com.acertainsupplychain.test.TestServer;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {
    
    static String serverAddress = "http://localhost:8081";//address of supplier
    static int supplierId = 1;
    static int port = 8081;
    static TestServer server;
    static Set<Integer> itemIds = new HashSet<Integer>();
    

    private static int intArg(String[] args, int argNum) throws IndexOutOfBoundsException {
        return Integer.parseInt(args[argNum]);
        
    }
    
    /*
     * The item set to be used
     */
    private static void intItemIds() {
        for(int i = 1; i < 21; i++){
            itemIds.add(i);
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int numConcurrentWorkloadThreadsStep = 1;
        int numConcurrentWorkloadThreadsMax = 25;
        int numRunsPerStep = 1;
        
        try {
            numConcurrentWorkloadThreadsMax = intArg(args,0);
            numRunsPerStep = intArg(args, 1);
            numConcurrentWorkloadThreadsMax = intArg(args, 2);
            
        } catch (IndexOutOfBoundsException e) {
            //Just ignore this
        }
        
        String fname = "benchmark.remote.dat";
        FileHandler logFile = new FileHandler(fname);
        Formatter datFormatter = new Formatter() {

            @Override
            public String format(LogRecord record) {
                AggregateResult result = (AggregateResult) record.getParameters()[0];
                StringBuffer sb = new StringBuffer(1000);
                sb.append(result.getWorkers());
                sb.append('\t');
                sb.append(result.getThroughput());
                sb.append('\t');
                sb.append(result.getGlobalClientXactRatio());
                sb.append('\t');
                sb.append(result.getsuccRatio());
                sb.append('\n');
                return sb.toString();
            }
            
            public String getHead(Handler h) {
                return "workers\tthroughput (succXact/s)\tglobalClientXactRatio\tsuccRatio\n";
            }
            
        };
        logFile.setFormatter(datFormatter);
        benchmarkLogger.addHandler(logFile);
        
        intItemIds();
        
        runTests(numConcurrentWorkloadThreadsStep, numConcurrentWorkloadThreadsMax, numRunsPerStep);
    }
    
    public static void runTests(int step, int max, int runs) throws Exception {
        List<WorkerRunResult> workerRunResults;
        List<Future<WorkerRunResult>> runResults;
        ExecutorService exec;
        WorkloadConfiguration config;
        Worker workerTask;
        
        //Run max/steps runs
        for(int j = step; 
                j < max+1; 
                j = j + step) {
            //Run the test numRunsPerStep times
            for (int k = 0; k < runs; k++) {
                //We want the same number of workers for all tests in this step
                exec = Executors
                        .newFixedThreadPool(j);
                        //Re-initialize the item supplier for each run
                        initializeItemSupplier(serverAddress);
                        
                        consoleLogger.info("Running with " + j + " workers, run #" + (k+1));
                        
                        workerRunResults = new ArrayList<WorkerRunResult>();
                        runResults = new ArrayList<Future<WorkerRunResult>>();
            
                        //Do a workload with j workers/threads.
                        for (int i = 0; i < j; i++) {
                            consoleLogger.info("Adding worker");
                            config = new WorkloadConfiguration(serverAddress, 8082+i, itemIds);
                            workerTask = new Worker(config);
                            // Keep the futures to wait for the result from the thread
                            runResults.add(exec.submit(workerTask));
                        }
                        
            
                        // Get the results from the threads using the futures returned
                        for (Future<WorkerRunResult> futureRunResult : runResults) {
                            
                            WorkerRunResult runResult = futureRunResult.get(); // blocking call
                            workerRunResults.add(runResult);
                        }

                        exec.awaitTermination(500, TimeUnit.MILLISECONDS); // shutdown the executor
                        reportMetric(workerRunResults);
                        exec = null;
                        
                        //must stop item supplier server after each step
                        //order manager servers and proxies are closed down by each worker.
                        consoleLogger.info("Stopping item supplier server for this step");
                        server.stopServer();
                        
                    }
                }
        consoleLogger.info("Benchmarking complete");
    }

    /**
     * Computes the metrics and prints them
     * 
     * @param workerRunResults
     */
    public static void reportMetric(List<WorkerRunResult> workerRunResults) {
        try {
                double aggregateThroughput = 0D;
                double succRatio = 0D;
                double globalXactRatio = 0D;
                int numWorkers = workerRunResults.size();
                double sumInteractions = 0D;
                double sumSuccInteractions = 0D;
                double sumGlobalInteractions = 0D;
                
                for(WorkerRunResult result : workerRunResults) {
                     int interactions = result.getSuccessfulInteractions();
                     double time = result.getElapsedTimeInNanoSecs()/1E9;
                     
                     aggregateThroughput += interactions/time;
                     sumInteractions += result.getTotalInteractions();
                     sumSuccInteractions += result.getSuccessfulInteractions();
                     sumGlobalInteractions += result.getTotalGlobalClientRuns();
                }
                
                succRatio = sumSuccInteractions / sumInteractions;
                globalXactRatio = sumGlobalInteractions / sumInteractions;
                
                Object[] parameters = {new AggregateResult(numWorkers, aggregateThroughput, succRatio, globalXactRatio)};
                LogRecord record = new LogRecord(Level.INFO, "Added result");
                record.setParameters(parameters);
                benchmarkLogger.log(record);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize item supplier before the workload interactions are run
     * 
     * @param serverAddress
     * @throws Exception
     */
    public static void initializeItemSupplier(String serverAddress) throws Exception {
        consoleLogger.info("Initializing supplier");
        ItemSupplier itemSupplier = 
                new CertainItemSupplier(supplierId, itemIds);
        ItemSupplierHTTPMessageHandler handler = 
                new ItemSupplierHTTPMessageHandler(itemSupplier);
        server = new TestServer(port);
        server.setHandler(handler);
        server.start();
        
        consoleLogger.info("Finished initializing supplier");
    }
    private static Logger consoleLogger = Logger.getLogger(CertainWorkload.class.getName() + "Console");
    private static Logger benchmarkLogger = Logger.getLogger(CertainWorkload.class.getName());
}
