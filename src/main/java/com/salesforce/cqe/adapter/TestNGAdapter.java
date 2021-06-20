package com.salesforce.cqe.adapter;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.salesforce.cqe.datamodel.client.TestSignalEnum;
import com.salesforce.cqe.datamodel.testng.Class;
import com.salesforce.cqe.datamodel.testng.Suite;
import com.salesforce.cqe.datamodel.testng.Test;
import com.salesforce.cqe.datamodel.testng.TestMethod;
import com.salesforce.cqe.datamodel.testng.TestngResults;
import com.salesforce.cqe.helper.ProcessException;

/**
 * @author
 * TestNG test result adapter class
 */
public class TestNGAdapter implements IAdapter {

    private static final String TESTNG_DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss z";

    @Override
    public TestNGRun process(InputStream testResultStream) throws ProcessException{
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

        List<ITestCase> testCaseList = new ArrayList<>();
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
                        List<ITestSignal> testSignalList = new ArrayList<>();
                        TestNGCase testCase = new TestNGCase(testCaseName,startTime,endTime,
                                                        method.getStatus(),testSignalList);
                        testCaseList.add(testCase); 
                        if (method.getException() == null) continue;
                        TestNGSignal signal = new TestNGSignal(TestSignalEnum.AUTOMATION.toString(),
                                                                method.getException().getClazz(),
                                                                endTime);
                        testSignalList.add(signal);  
                        
                    }
                }
            }
        }
        return new TestNGRun(testSuiteName,"",suiteStart,suiteEnd,testCaseList);
    }

    private ZonedDateTime getDatetime(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TESTNG_DATEFORMAT);
        return ZonedDateTime.parse(timestamp,formatter);
    }
    
}
