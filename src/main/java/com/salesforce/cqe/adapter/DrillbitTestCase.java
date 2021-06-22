package com.salesforce.cqe.adapter;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Yibing Tao
 * ITestCase interface defines how to get test case result
 */
public interface DrillbitTestCase {
    public String getTestCaseFullName(); 
    public ZonedDateTime getTestCaseStartTime();
    public ZonedDateTime getTestCaseEndTime();
    public String getTestCaseStatus();
    public List<DrillbitTestSignal> getTestSignalList();
}
