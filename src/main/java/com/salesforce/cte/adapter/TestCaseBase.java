package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.List;

/**
 * @author Yibing Tao
 * Base class for test case result
 */
public class TestCaseBase implements TestAdvisorTestCase {

    private String testCaseFullName;
    private Instant testCaseStartTime;
    private Instant testCaseEndTime;
    private String  testCaseStatus;
    private List<TestAdvisorTestSignal> testCaseSignalList;

    public TestCaseBase(String name, Instant start, Instant end, 
                    String status, List<TestAdvisorTestSignal> signalList){
        testCaseFullName = name;
        testCaseStartTime = start;
        testCaseEndTime = end;
        testCaseStatus = status;
        testCaseSignalList = signalList;
    }

    @Override
    public String getTestCaseFullName() {
        return testCaseFullName;
    }

    @Override
    public Instant getTestCaseStartTime() {
        return testCaseStartTime;
    }

    @Override
    public Instant getTestCaseEndTime() {
        return testCaseEndTime;
    }

    @Override
    public String getTestCaseStatus() {
        return testCaseStatus;
    }

    @Override
    public List<TestAdvisorTestSignal> getTestSignalList() {
        return testCaseSignalList;
    }
    
}
