package com.salesforce.cqe.processor;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.salesforce.cqe.adapter.DrillbitAdapter;
import com.salesforce.cqe.adapter.DrillbitTestCase;
import com.salesforce.cqe.adapter.DrillbitTestRun;
import com.salesforce.cqe.adapter.DrillbitTestSignal;
import com.salesforce.cqe.datamodel.client.TestExecution;
import com.salesforce.cqe.datamodel.client.TestRunSignal;
import com.salesforce.cqe.datamodel.client.TestSignal;
import com.salesforce.cqe.datamodel.client.TestSignalEnum;
import com.salesforce.cqe.datamodel.client.TestStatus;
import com.salesforce.cqe.helper.ProcessException;

/**
 * @author Yibing Tao
 * This class provide method to process test result with the provided adapter
 */
public class Processor {

    private Processor(){
        //empty private constructor to hide default one and prevent instance
    }

    /**
     * 
     * @param inputStream 
     * stream to access test result file
     * @param testRunSignal 
     * test run signals
     * @param adapter 
     * adapter to convert test result to data model CLI can use
     * @throws ProcessException 
     * when any process error happened
     */
    public static void process(InputStream inputStream, TestRunSignal testRunSignal,DrillbitAdapter adapter) 
                            throws ProcessException{
        DrillbitTestRun testRun = adapter.process(inputStream);
        testRunSignal.buildStartTime = testRun.getTestSuiteStartTime().format(DateTimeFormatter.ISO_INSTANT);
        testRunSignal.buildEndTime = testRun.getTestSuiteEndTime().format(DateTimeFormatter.ISO_INSTANT);
        testRunSignal.testSuiteName = testRunSignal.testSuiteName.isEmpty() ? testRun.getTestSuiteName() : testRunSignal.testSuiteName;
        testRunSignal.clientBuildId = testRunSignal.clientBuildId.isEmpty() ? testRun.getTestsSuiteInfo() : testRunSignal.clientBuildId;
        testRunSignal.testExecutions = new ArrayList<>();
        for(DrillbitTestCase testCase : testRun.getTestCaseList()){
            TestExecution testExection = new TestExecution();
            testExection.testCaseName = testCase.getTestCaseFullName();
            testExection.startTime = testCase.getTestCaseStartTime().format(DateTimeFormatter.ISO_INSTANT);
            testExection.endTime = testCase.getTestCaseEndTime().format(DateTimeFormatter.ISO_INSTANT);
            testExection.status = TestStatus.valueOf(testCase.getTestCaseStatus());
            testExection.testSignals = new ArrayList<>();
            for(DrillbitTestSignal signal : testCase.getTestSignalList()){
                TestSignal testSignal = new TestSignal();
                testSignal.signalName = TestSignalEnum.valueOf(signal.getTestSignalName());
                testSignal.signalValue = signal.getTestSignalValue();
                testSignal.signalTime = signal.getTestSignalTime().format(DateTimeFormatter.ISO_INSTANT);
                testExection.testSignals.add(testSignal);
            }
            testRunSignal.testExecutions.add(testExection);
        } 
    }

}
