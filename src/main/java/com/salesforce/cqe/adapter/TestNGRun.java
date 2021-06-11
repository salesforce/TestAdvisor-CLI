package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * TestNG test run result
 */
public class TestNGRun implements ITestRun {
    private String testSuiteName="";
    private String testSuiteInfo="";
    private ZonedDateTime testSuiteStartTime;
    private ZonedDateTime testSuiteEndTime;
    private List<ITestCase> testCaseList;
  
    public TestNGRun(String name, String info, ZonedDateTime start, 
                    ZonedDateTime end, List<ITestCase> caseList){
        testSuiteName = name;
        testSuiteInfo = info;
        testSuiteStartTime = start;
        testSuiteEndTime = end;
        testCaseList = caseList;
    }

    @Override
    public String getTestSuiteName() {
        return testSuiteName;
    }

    @Override
    public String getTestsSuiteInfo() {
        return testSuiteInfo;
    }

    @Override
    public ZonedDateTime getTestSuiteStartTime() {
        return testSuiteStartTime;
    }

    @Override
    public ZonedDateTime getTestSuiteEndTime() {
        return testSuiteEndTime;
    }

    @Override
    public List<ITestCase> getTestCaseList() {
        return testCaseList;
    }

    
}
