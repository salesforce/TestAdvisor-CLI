/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import com.salesforce.cte.common.TestEventType;

import org.testng.annotations.Test;

public class TestSignalTest {
    
    @Test
    public void testInvalidUrl(){
        String url = "invalid url";
        TestSignalBase signal = new TestSignalBase(TestEventType.URL, url, Instant.now());

        assertEquals("", signal.getTestSignalValue());
    }

    @Test
    public void testRemoveQueryParameters(){
        String url = "http://test.org/query?p1=v1";
        TestSignalBase signal = new TestSignalBase(TestEventType.URL, url, Instant.now());

        assertEquals("http://test.org/query", signal.getTestSignalValue());
    }


}
