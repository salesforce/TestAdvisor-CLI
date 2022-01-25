/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.salesforce.cte.common.TestAdvisorResult;
import com.salesforce.cte.common.TestCaseExecution;
import com.salesforce.cte.common.TestEvent;
import com.salesforce.cte.helper.ProcessException;

public class TestAdvisorResultAdapter implements TestAdvisorAdapter {

    @Override
    public TestAdvisorTestRun process(InputStream input) throws ProcessException {
        
        TestAdvisorResult testAdvisorResult;
        try {
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                                      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            testAdvisorResult = objectMapper.readValue(input, new TypeReference<TestAdvisorResult>(){});
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        
        List<TestAdvisorTestCase> testCaseList = new ArrayList<>();
        for(TestCaseExecution testExecution : testAdvisorResult.testCaseExecutionList){
            List<TestAdvisorTestSignal> testSignalList = new ArrayList<>();
            for(TestEvent event : testExecution.eventList){
                testSignalList.add(new TestSignalBase(event.getEventSource(),
                            event.getEventContent(),event.getEventTime(), event.getEventLevel(), event.getSeleniumCmd(),
                            event.getSeleniumCmdParam(),event.getSeleniumLocator(), event.getScreenshotRecordNumber(),
                            event.getScreenshotPath()));
            }
            
            testCaseList.add(new TestCaseBase(testExecution.getTestName(),testExecution.startTime,testExecution.endTime,
                            testExecution.getTestStatus().toString(),testExecution.getTraceId(), testSignalList));
        }

        return new TestRunBase("","",testAdvisorResult.version,testAdvisorResult.buildStartTime,testAdvisorResult.buildEndTime,testCaseList);
    }

    
}
