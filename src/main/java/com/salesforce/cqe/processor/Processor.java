package com.salesforce.cqe.processor;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.salesforce.cqe.datamodel.client.TestExecution;
import com.salesforce.cqe.datamodel.client.TestRunSignal;
import com.salesforce.cqe.datamodel.client.TestSignal;
import com.salesforce.cqe.datamodel.client.TestSignalEnum;
import com.salesforce.cqe.datamodel.client.TestStatus;
import com.salesforce.cqe.datamodel.testng.Class;
import com.salesforce.cqe.datamodel.testng.Suite;
import com.salesforce.cqe.datamodel.testng.Test;
import com.salesforce.cqe.datamodel.testng.TestMethod;
import com.salesforce.cqe.datamodel.testng.TestngResults;


public class Processor {

    private Processor(){
        //To hide default constructor and ensure on intance allowed
    }

    public static TestRunSignal processTestNGSignal(InputStream inputStream, TestRunSignal testRunSignal) 
                                throws JAXBException{
        JAXBContext context = JAXBContext.newInstance(TestngResults.class);
        TestngResults testResults = (TestngResults) context.createUnmarshaller().unmarshal(inputStream);

        testRunSignal.testExecutions = new ArrayList<>();
        ZonedDateTime suiteStart = ZonedDateTime.now().plusYears(100); //first test suite start time
        ZonedDateTime suiteEnd = ZonedDateTime.now().minusYears(100); //last test suite end time

        for(Suite suite : testResults.getSuite()){
            suiteStart = suiteStart.isBefore(getDatetime(suite.getStartedAt())) 
                            ?  suiteStart : getDatetime(suite.getStartedAt());
            suiteEnd = suiteEnd.isAfter(getDatetime(suite.getFinishedAt())) 
                            ?  suiteEnd : getDatetime(suite.getFinishedAt());
            for(Test test : suite.getTest()){
                for(Class cls : test.getClazz()){
                    for(TestMethod method : cls.getTestMethod()){
                        TestExecution testExecution = new TestExecution();
                        testExecution.startTime = getDatetime(method.getStartedAt()).format(DateTimeFormatter.ISO_INSTANT);
                        testExecution.endTime = getDatetime(method.getFinishedAt()).format(DateTimeFormatter.ISO_INSTANT);
                        testExecution.status = TestStatus.valueOf(method.getStatus());
                        testExecution.testCaseName = cls.getName() + "." + method.getName();
                        if (method.getException() != null)
                        {
                            testExecution.testSignals = new ArrayList<>();
                            TestSignal signal = new TestSignal();
                            signal.signalName = TestSignalEnum.AUTOMATION;
                            signal.signalValue = method.getException().getClazz();
                            signal.signalTime = testExecution.endTime;
                            testExecution.testSignals.add(signal);
                        }    
                        testRunSignal.testExecutions.add(testExecution);   
                    }
                }
            }
        }
        testRunSignal.buildEndTime = suiteEnd.format(DateTimeFormatter.ISO_INSTANT);
        testRunSignal.buildStartTime = suiteStart.format(DateTimeFormatter.ISO_INSTANT);
        return testRunSignal;
    }
  
    private static ZonedDateTime getDatetime(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss z");
        return ZonedDateTime.parse(timestamp,formatter);
    }
}
