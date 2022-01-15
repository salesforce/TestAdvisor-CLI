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
    private String traceId;
    private List<TestAdvisorTestSignal> testCaseSignalList;

    public TestCaseBase(String name, Instant start, Instant end, 
                    String status, List<TestAdvisorTestSignal> signalList){
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
    
}
