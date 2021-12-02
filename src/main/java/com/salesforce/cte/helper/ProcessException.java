/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.helper;

/**
 * @author Yibing Tao
 * ProcessException will throw when adapter failed to process the test result.
 * The cause will be the actual exception was throw.
 */
public class ProcessException extends Exception {
    public ProcessException(Throwable cause){
        super(cause);
    }
}
