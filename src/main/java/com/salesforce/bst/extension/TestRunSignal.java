package com.salesforce.bst.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao
 * This class defines all properties as signals to indicate potential regressions
 * for a test run.
 * Regression signals could be collected from test run or Salesforce internal system.
 */
public class TestRunSignal {
    @JsonProperty
    private Customer customer;
    @JsonProperty
    private Registry registry; 
    
    private String testRunRecordId;
    // both start and end time are define as the time difference in milliseconds between
    // the current time and midnight, January 1, 1970, UTC
    @JsonProperty
    private long   startTime; 
    @JsonProperty
    private long   endTime;
    @JsonProperty
    private List<TestSignal> testSignals;

    public TestRunSignal(Customer customer, Registry registry, long start, long end){
        this.customer = customer;
        this.registry = registry;
        this.startTime = start;
        this.endTime = end;
        this.testSignals = new ArrayList<>();
    }

    public Customer getCustomer(){
        return this.customer;
    }

    public Registry getRegistry(){
        return this.registry;
    }

    public String getTestRunRecordId(){
        return this.testRunRecordId;
    }
    public void setTestRunRecordId(String id){
        this.testRunRecordId = id;
    }

    public long getStartTime(){
        return this.startTime;
    }

    public long getEndTime(){
        return this.endTime;
    }

    public List<TestSignal> getTestSignals(){
        return this.testSignals;
    }
}
