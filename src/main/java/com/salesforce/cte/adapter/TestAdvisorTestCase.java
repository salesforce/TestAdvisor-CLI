package com.salesforce.cte.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * ITestCase interface defines how to get test case result
 */
public interface TestAdvisorTestCase {
    public String getTestCaseFullName(); 
    public ZonedDateTime getTestCaseStartTime();
    public ZonedDateTime getTestCaseEndTime();
    public String getTestCaseStatus();
    public List<TestAdvisorTestSignal> getTestSignalList();
}
