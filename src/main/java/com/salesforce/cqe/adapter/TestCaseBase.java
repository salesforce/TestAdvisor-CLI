package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * Base class for test case result
 */
public class TestCaseBase implements DrillbitTestCase {

    private String testCaseFullName;
    private ZonedDateTime testCaseStartTime;
    private ZonedDateTime testCaseEndTime;
    private String  testCaseStatus;
    private List<DrillbitTestSignal> testCaseSignalList;

    public TestCaseBase(String name, ZonedDateTime start, ZonedDateTime end, 
                    String status, List<DrillbitTestSignal> signalList){
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
    public ZonedDateTime getTestCaseStartTime() {
        return testCaseStartTime;
    }

    @Override
    public ZonedDateTime getTestCaseEndTime() {
        return testCaseEndTime;
    }

    @Override
    public String getTestCaseStatus() {
        return testCaseStatus;
    }

    @Override
    public List<DrillbitTestSignal> getTestSignalList() {
        return testCaseSignalList;
    }
    
}
