package com.salesforce.cte.adapter;

import java.time.Instant;
import java.util.List;

/**
 * @author Yibing Tao
 * ITestCase interface defines how to get test case result
 */
public interface TestAdvisorTestCase {
    public String getTestCaseFullName(); 
    public Instant getTestCaseStartTime();
    public Instant getTestCaseEndTime();
    public String getTestCaseStatus();
    public List<TestAdvisorTestSignal> getTestSignalList();
}
