/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

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
    private boolean isConfiguration;
    private String traceId;
    private List<TestAdvisorTestSignal> testCaseSignalList;

    public TestCaseBase(String name, Instant start, Instant end, 
                    String status, Boolean isConfiguration, List<TestAdvisorTestSignal> signalList){
        testCaseFullName = name;
        testCaseStartTime = start;
        testCaseEndTime = end;
        testCaseStatus = status;
        this.isConfiguration = isConfiguration;
        testCaseSignalList = signalList;
    }

    public TestCaseBase(String name, Instant start, Instant end, 
                    String status, boolean isConfiguration, String traceId, List<TestAdvisorTestSignal> signalList){
        this(name,start,end,status, isConfiguration, signalList);
        this.traceId=traceId;
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

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public boolean getIsConfiguration() {
        return isConfiguration;
    }
    
}
