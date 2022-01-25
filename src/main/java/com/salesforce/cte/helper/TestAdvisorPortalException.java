/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.helper;

/**
 * @author Yibing Tao
 * TestAdvisorPortalException will throw when CLI request was rejected by
 * testadvisor portal. The message will be the portal response.
 */
public class TestAdvisorPortalException extends Exception {
    public TestAdvisorPortalException(String message){
        super(message);
    }

    public TestAdvisorPortalException(Exception ex){
        super(ex);
    }
}
