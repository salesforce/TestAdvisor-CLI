package com.salesforce.bst.extension;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao
 * This class defines all properties as signals to indicate potential regressions
 * for a test case.
 * Regression signals could be collected from test run or Salesforce internal system.
 */
public class TestSignal {
    
    @JsonProperty
    private String  testName; // Regex "[a-zA-Z0-9]{1,80}"
    // both start and end time are define as the time difference in milliseconds between
    // the current time and midnight, January 1, 1970, UTC
    @JsonProperty
    private long    startTime;
    @JsonProperty
    private long    endTime;
    @JsonProperty
    private TestResult testResult;
    @JsonProperty
    private List<TestFailure> testFailures; // test failures collected from all sources

    public TestSignal(String name, long start, long end, TestResult result){
        this.testName = name;
        this.startTime = start;
        this.endTime = end;
        this.testResult = result;
    }

    public String getTestName(){
        return this.testName;
    }

    public long getStartTime(){
        return this.startTime;
    }

    public long getEndTime(){
        return this.endTime;
    }

    public TestResult getTestResult(){
        return this.testResult;
    }

    public List<TestFailure> getTestFailures(){
        return this.testFailures;
    }
    public void setTestFailures(List<TestFailure> testFailures){
        this.testFailures = testFailures;
    }

}
