package com.acertainsupplychain.utils;

import java.util.List;
import java.util.Set;

/*
 * A skeleton for the communication responses.
 * This includes carrying results and exceptions.
 */
public class CommunicationResponse {
    private Exception exception;
    private List<?> list;
    private Set<?> set;
    private Integer id;

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    /*
     * List can be of type OrderStep, StepStatus and ItemQuantity
     */
    public CommunicationResponse(Exception exception, List<?> list) {
        this.setException(exception);
        this.setList(list);
    }
    
    public CommunicationResponse(Exception exception, Set<Integer> set) {
        this.setException(exception);
        this.setSet(set);
    }
    

    public CommunicationResponse() {
        this.setException(null);
        this.setList(null);
        this.setSet(null);
        this.setId(null);
    }

    public List<?> getList() {
        return list;
    }

    public void setList(List<?> list) {
        this.list = list;
    }
    
    public Set<?> getSet() {
        return set;
    }

    public void setSet(Set<?> set) {
        this.set = set;
    }
    
    public int getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
}
