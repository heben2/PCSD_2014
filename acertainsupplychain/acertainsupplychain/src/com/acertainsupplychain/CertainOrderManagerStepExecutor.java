package com.acertainsupplychain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainOrderManagerStepExecutor {

	ExecutorService exec;
	
	public CertainOrderManagerStepExecutor(int maxReplicatorThreads) {
		exec = Executors.newFixedThreadPool(maxReplicatorThreads);
	}

	public List<Future<StepExecutionResult>> executeStep 
	    (Set<StepExecutionRequest> executionRequests) throws Exception {
		
		List<Future<StepExecutionResult>> results = new ArrayList<Future<StepExecutionResult>>();
		for(StepExecutionRequest request : executionRequests){
		    CertainOrderManagerStepExecutionTask task = new CertainOrderManagerStepExecutionTask(request);
            results.add(exec.submit(task));
		}
		return results;
	}
	
	/*
	 * Kill the executor. All tasks are thrown away.
	 */
	public void stop(){
	    exec.shutdownNow();
	}

}
