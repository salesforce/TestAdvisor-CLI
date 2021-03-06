/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.helper;

/**
 * @author Yibing Tao
 * This exception will throw when TestAdvisor CLI secrets manager encounter any
 * cihper related exception. The cause will be the actual exception was throw.
 */
public class TestAdvisorCipherException extends Exception {
    public TestAdvisorCipherException(Throwable cause){
        super(cause);
    }
}