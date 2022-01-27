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
 * ITestCase interface defines how to get test case result
 */
public interface TestAdvisorTestCase {
    public String getTestCaseFullName(); 
    public Instant getTestCaseStartTime();
    public Instant getTestCaseEndTime();
    public String getTestCaseStatus();
    public String getTraceId();
    public List<TestAdvisorTestSignal> getTestSignalList();
}
