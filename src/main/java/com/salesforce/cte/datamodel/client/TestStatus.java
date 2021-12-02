/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.datamodel.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yibing Tao
 */
public enum TestStatus {
    @JsonProperty("Failed")
    FAIL,
    @JsonProperty("Passed")
    PASS,
    @JsonProperty("Skipped")
    SKIP
}
