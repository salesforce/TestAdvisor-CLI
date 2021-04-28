package com.salesforce.bst.processor;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.salesforce.bst.datamodel.client.Customer;
import com.salesforce.bst.datamodel.client.Registry;
import com.salesforce.bst.datamodel.client.TestResult;
import com.salesforce.bst.datamodel.client.TestRunSignal;
import com.salesforce.bst.datamodel.client.TestSignal;
import com.salesforce.bst.datamodel.testng.Suite;
import com.salesforce.bst.datamodel.testng.Test;
import com.salesforce.bst.datamodel.testng.TestMethod;
import com.salesforce.bst.datamodel.testng.Class;
import com.salesforce.bst.datamodel.testng.TestngResults;


public class Processor {

    public static TestRunSignal processTestNGSignal(InputStream inputStream, Customer customer, Registry registry) 
                                throws JAXBException, ParseException{
        JAXBContext context = JAXBContext.newInstance(TestngResults.class);
        TestngResults testResults = (TestngResults) context.createUnmarshaller().unmarshal(inputStream);

        List<TestSignal> testSignals = new ArrayList<>();
        long suiteStart=Long.MAX_VALUE; //first test suite start time
        long suiteEnd=0; //last test suite end time

        for(Suite suite : testResults.getSuite()){
            suiteStart = Math.min(suiteStart, toMillis(suite.getStartedAt()));
            suiteEnd = Math.max(suiteEnd, toMillis(suite.getFinishedAt()));
            for(Test test : suite.getTest()){
                for(Class cls : test.getClazz()){
                    for(TestMethod method : cls.getTestMethod()){
                        TestSignal signal = new TestSignal(cls.getName() + "." + method.getName(), 
                                                            toMillis(method.getStartedAt()), 
                                                            toMillis(method.getFinishedAt()), 
                                                            TestResult.valueOf(method.getStatus()));
                        testSignals.add(signal);
                    }
                }
            }
        }

        return new TestRunSignal(customer,registry,suiteStart,suiteEnd,testSignals);
    }

    
    private static long toMillis(String timestamp) throws ParseException{
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
        Date date = formatter.parse(timestamp);
        return date.getTime();
    }
}
