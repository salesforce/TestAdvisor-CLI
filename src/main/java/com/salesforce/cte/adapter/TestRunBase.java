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
 * Base class for test run result
 */
public class TestRunBase implements TestAdvisorTestRun {
    private String testSuiteName="";
    private String testSuiteInfo="";
    private String testAdvisorVersion="";
    private Instant testSuiteStartTime;
    private Instant testSuiteEndTime;
    private List<TestAdvisorTestCase> testCaseList;
  
    public TestRunBase(String name, String info, String version, Instant start, 
                    Instant end, List<TestAdvisorTestCase> caseList){
        testSuiteName = name;
        testSuiteInfo = info;
        testAdvisorVersion = version;
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
    public Instant getTestSuiteStartTime() {
        return testSuiteStartTime;
    }

    @Override
    public Instant getTestSuiteEndTime() {
        return testSuiteEndTime;
    }

    @Override
    public List<TestAdvisorTestCase> getTestCaseList() {
        return testCaseList;
    }

    @Override
    public String getTestAdvisorVersion() {
        return testAdvisorVersion;
    }

    
}
