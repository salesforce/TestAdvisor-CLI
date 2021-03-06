/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.adapter;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.salesforce.cte.common.TestEventType;
import com.salesforce.cte.datamodel.testng.Class;
import com.salesforce.cte.datamodel.testng.Suite;
import com.salesforce.cte.datamodel.testng.Test;
import com.salesforce.cte.datamodel.testng.TestMethod;
import com.salesforce.cte.datamodel.testng.TestngResults;
import com.salesforce.cte.helper.ProcessException;

/**
 * @author
 * TestNG test result adapter class
 */
public class TestNGAdapter implements TestAdvisorAdapter {

    private static final String TESTNG_DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss z";

    @Override
    public TestRunBase process(InputStream testResultStream) throws ProcessException{
        JAXBContext context;
        TestngResults testResults;
        try {
            context = JAXBContext.newInstance(TestngResults.class);
            testResults = (TestngResults) context.createUnmarshaller().unmarshal(testResultStream);
        } catch (JAXBException e) {
            throw new ProcessException(e);
        }

        ZonedDateTime suiteStart = ZonedDateTime.now().plusYears(100); //first test suite start time
        ZonedDateTime suiteEnd = ZonedDateTime.now().minusYears(100); //last test suite end time
        String testSuiteName="";

        List<TestAdvisorTestCase> testCaseList = new ArrayList<>();
        for(Suite suite : testResults.getSuite()){
            testSuiteName = suite.getName();
            suiteStart = suiteStart.isBefore(getDatetime(suite.getStartedAt())) 
                            ?  suiteStart : getDatetime(suite.getStartedAt());
            suiteEnd = suiteEnd.isAfter(getDatetime(suite.getFinishedAt())) 
                            ?  suiteEnd : getDatetime(suite.getFinishedAt());
            for(Test test : suite.getTest()){
                for(Class cls : test.getClazz()){
                    for(TestMethod method : cls.getTestMethod()){                      
                        ZonedDateTime startTime = getDatetime(method.getStartedAt());
                        ZonedDateTime endTime = getDatetime(method.getFinishedAt());
                        String testCaseName = cls.getName() + "." + method.getName();
                        List<TestAdvisorTestSignal> testSignalList = new ArrayList<>();
                        TestCaseBase testCase = new TestCaseBase(testCaseName,startTime.toInstant(),endTime.toInstant(),
                                                        method.getStatus(),false, 0, testSignalList);
                        testCaseList.add(testCase); 
                        if (method.getException() == null) continue;
                        TestSignalBase signal = new TestSignalBase(TestEventType.AUTOMATION,
                                                                method.getException().getClazz(),
                                                                endTime.toInstant());
                        testSignalList.add(signal);  
                        
                    }
                }
            }
        }
        return new TestRunBase(testSuiteName,"","",suiteStart.toInstant(),suiteEnd.toInstant(),testCaseList);
    }

    private ZonedDateTime getDatetime(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TESTNG_DATEFORMAT);
        return ZonedDateTime.parse(timestamp,formatter);
    }
    
}
