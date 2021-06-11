package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * TestNG test case result
 */
public class TestNGCase implements ITestCase {

    private String testCaseFullName;
    private ZonedDateTime testCaseStartTime;
    private ZonedDateTime testCaseEndTime;
    private String  testCaseStatus;
    private List<ITestSignal> testCaseSignalList;

    public TestNGCase(String name, ZonedDateTime start, ZonedDateTime end, 
                    String status, List<ITestSignal> signalList){
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
    public List<ITestSignal> getTestSignalList() {
        return testCaseSignalList;
    }
    
}
