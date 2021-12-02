/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.datamodel.client;

/**
 * @author Yibing Tao
 * Defines test signal type name 
 * Each test signal enum value represents signals collected by a type of event listener
 */
public enum TestSignalEnum {
    AUTOMATION, //Test signal collected by Test Provider Listener
    SELENIUM    // Test signal collected by Selenium Event Listener
}
