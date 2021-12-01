/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import java.io.InputStream;

import com.salesforce.cte.helper.ProcessException;

/**
 * @author Yibing Tao
 * Adapter interface to process test result
 */
public interface TestAdvisorAdapter {
    public TestAdvisorTestRun process(InputStream input) throws ProcessException;
}
