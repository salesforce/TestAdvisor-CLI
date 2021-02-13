package com.salesforce.bst.extension;

import java.util.List;

public class TestSignal {
    String  testName;
    long    startTime;
    long    endTime;
    TestResult testResult;
    Regression testRegression;
    List<TestFailure> testFailures;
    List<TestFailure> seleniumFailures;
    List<TestFailure> consoleFailures;
    List<TestFailure> screenshotFailures;
}
