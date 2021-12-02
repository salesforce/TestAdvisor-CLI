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
