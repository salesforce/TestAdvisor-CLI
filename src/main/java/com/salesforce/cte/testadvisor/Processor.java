package com.salesforce.cte.testadvisor;

import java.io.InputStream;
import java.util.ArrayList;

import com.salesforce.cte.adapter.TestAdvisorAdapter;
import com.salesforce.cte.adapter.TestAdvisorTestCase;
import com.salesforce.cte.adapter.TestAdvisorTestRun;
import com.salesforce.cte.adapter.TestAdvisorTestSignal;
import com.salesforce.cte.datamodel.client.TestExecution;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestSignal;
import com.salesforce.cte.datamodel.client.TestStatus;
import com.salesforce.cte.helper.ProcessException;

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
    public static void process(InputStream inputStream, TestRunSignal testRunSignal,TestAdvisorAdapter adapter) 
                            throws ProcessException{
        TestAdvisorTestRun testRun = adapter.process(inputStream);
        testRunSignal.buildStartTime = testRun.getTestSuiteStartTime() == null ? null : testRun.getTestSuiteStartTime();
        testRunSignal.buildEndTime = testRun.getTestSuiteEndTime() == null ? null : testRun.getTestSuiteEndTime();
        testRunSignal.clientLibraryVersion = testRun.getTestAdvisorVersion();
        //testRunSignal.clientCliVersion = CLI.class.getClass().getPackage().getImplementationVersion();
        testRunSignal.testSuiteName = testRunSignal.testSuiteName.isEmpty() ? testRun.getTestSuiteName() : testRunSignal.testSuiteName;
        testRunSignal.clientBuildId = testRunSignal.clientBuildId.isEmpty() ? testRun.getTestsSuiteInfo() : testRunSignal.clientBuildId;
        testRunSignal.testExecutions = new ArrayList<>();
        for(TestAdvisorTestCase testCase : testRun.getTestCaseList()){
            TestExecution testExection = new TestExecution();
            testExection.testCaseName = testCase.getTestCaseFullName();
            testExection.startTime = testCase.getTestCaseStartTime();
            testExection.endTime = testCase.getTestCaseEndTime();
            testExection.status = enumPartialMatch(TestStatus.class, testCase.getTestCaseStatus());
            testExection.testSignals = new ArrayList<>();
            for(TestAdvisorTestSignal signal : testCase.getTestSignalList()){
                TestSignal testSignal = new TestSignal();
                testSignal.signalName = signal.getTestSignalName();
                testSignal.signalValue = signal.getTestSignalValue();
                testSignal.signalTime = signal.getTestSignalTime();
                testExection.testSignals.add(testSignal);
            }
            testRunSignal.testExecutions.add(testExection);
        } 
    }

    public static <T extends Enum<?>> T enumPartialMatch(Class<T> enumeration, String search) {
        for (T each : enumeration.getEnumConstants()) {
            if (search.toUpperCase().contains(each.name().toUpperCase())) {
                return each;
            }
        }
        return null;
    }

}
