package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.List;

/**
 * @author Yibing Tao
 * ITestRun interface define how to get test run result
 */
public interface TestAdvisorTestRun {
    public String getTestSuiteName(); 
    public String getTestsSuiteInfo(); 
    public String getTestAdvisorVersion();
    public Instant getTestSuiteStartTime(); 
    public Instant getTestSuiteEndTime();  
    public List<TestAdvisorTestCase> getTestCaseList();
}
