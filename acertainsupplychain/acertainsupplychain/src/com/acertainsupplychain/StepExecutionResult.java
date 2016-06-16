package com.acertainsupplychain;

/**
 * StepExecutionResult represents the result of a step execution for a workflow
 * on an item supplier.
 */
public class StepExecutionResult {
    private boolean failStop = false;
	private boolean stepExecutionSuccessful;
	private int workflowId; //can only be set once.
	private OrderStep step; //can only be set once.

	public StepExecutionResult(int workflowId, OrderStep step, boolean stepExecutionSuccessful) {
		this.setStepExecutionSuccessful(stepExecutionSuccessful);
		this.workflowId = workflowId;
		this.step = step;
	}

	public boolean isStepExecutionSuccessful() {
		return stepExecutionSuccessful;
	}

	public void setStepExecutionSuccessful(boolean stepExecutionSuccessful) {
		this.stepExecutionSuccessful = stepExecutionSuccessful;
	}
	
	public int getWorkflowId(){
	    return workflowId;
	}
	
	public OrderStep getStep(){
        return step;
    }
	
	public void setFailStop(boolean failStop){
	    this.failStop = failStop;
	}
	
	public boolean isFailStop(){
        return failStop;
    }

}
