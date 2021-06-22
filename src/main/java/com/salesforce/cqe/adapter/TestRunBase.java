package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * Base class for test run result
 */
public class TestRunBase implements DrillbitTestRun {
    private String testSuiteName="";
    private String testSuiteInfo="";
    private ZonedDateTime testSuiteStartTime;
    private ZonedDateTime testSuiteEndTime;
    private List<DrillbitTestCase> testCaseList;
  
    public TestRunBase(String name, String info, ZonedDateTime start, 
                    ZonedDateTime end, List<DrillbitTestCase> caseList){
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
    public List<DrillbitTestCase> getTestCaseList() {
        return testCaseList;
    }

    
}
