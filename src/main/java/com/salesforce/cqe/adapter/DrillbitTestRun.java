package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * ITestRun interface define how to get test run result
 */
public interface DrillbitTestRun {
    public String getTestSuiteName(); 
    public String getTestsSuiteInfo(); 
    public ZonedDateTime getTestSuiteStartTime(); 
    public ZonedDateTime getTestSuiteEndTime();  
    public List<DrillbitTestCase> getTestCaseList();
}
