package com.acertainsupplychain;

public class StepExecutionRequest {
    private final String serverAddress;
    private final Integer managerId;
    private final int workflowId;
    private final OrderStep step;
    
    public StepExecutionRequest(String serverAddress, Integer managerId, int workflowId, OrderStep step){
        this.serverAddress = serverAddress;
        this.managerId = managerId;
        this.workflowId = workflowId;
        this.step = step;
    }
    
    public String getServerAddres(){
        return serverAddress;
    }
    
    public int getManagerId(){
        return managerId;
    }
    
    public int getWorkflowId(){
        return workflowId;
    }
    
    public OrderStep getOrderStep(){
        return step;
    }
}
