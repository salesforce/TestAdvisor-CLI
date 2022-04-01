/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;


import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.romankh3.image.comparison.model.Rectangle;
import com.salesforce.cte.common.TestAdvisorResult;
import com.salesforce.cte.common.TestCaseExecution;
import com.salesforce.cte.common.TestEventType;
import com.salesforce.cte.datamodel.client.TestExecution;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestSignal;
import com.salesforce.cte.datamodel.client.TestStatus;

/**
 * Helper class to manage registry for test
 * @author Yibing Tao
 */
public class RegistryHelper {
    
    public static Path createTestRun(Registry registry, int plusSeconds) throws StreamWriteException, DatabindException, IOException{
        DateTimeFormatter taDateFormatter = DateTimeFormatter.ofPattern(Registry.TESTADVISOR_TESTRUN_PATTERN_STRING);
        String testRunId = Registry.TESTADVISOR_TESTRUN_PREFIX + taDateFormatter.format(
            OffsetDateTime.now( ZoneOffset.UTC ).plusSeconds(plusSeconds));
        Path testrun = registry.getRegistryRoot().resolve(testRunId);
        testrun.toFile().mkdirs();

        return testrun;
    }

    public static TestRunSignal createTestRunSignal(String testrunId){
        TestRunSignal testRunSignal = new TestRunSignal();
        Instant now = Instant.now();
        testRunSignal.testRunId = testrunId;
        testRunSignal.buildStartTime = now;
        testRunSignal.buildEndTime = now.plusSeconds(5);
        testRunSignal.clientBuildId = "123";
        testRunSignal.clientCliVersion = "1.0.1";
        testRunSignal.clientLibraryVersion = "1.0.1";
        testRunSignal.clientRegistryGuid = UUID.randomUUID();
        testRunSignal.sandboxInstance = "CS997";
        testRunSignal.sandboxOrgId = "00D9A0000009IsD";
        testRunSignal.sandboxOrgName = "bst";
        testRunSignal.testSuiteName = "testSuite1";
        testRunSignal.testExecutions = new ArrayList<>();

        TestExecution testExecution = createTestExecution("testcaseFail",TestStatus.FAIL);
        testExecution.testSignals.add(createTestSignal(TestEventType.TEST_EXCEPTION,"Exception"));
        testExecution.testSignals.add(createTestSignal(TestEventType.AUTOMATION,"Exception"));
        testRunSignal.testExecutions.add(testExecution);

        testExecution = createTestExecution("testcasePass",TestStatus.PASS);
        testExecution.testSignals.add(createTestSignal(TestEventType.TEST_EXCEPTION,"Exception"));
        testExecution.testSignals.add(createTestSignal(TestEventType.AUTOMATION,"Exception"));
        testRunSignal.testExecutions.add(testExecution);

        testExecution = createTestExecution("testcaseWithRedundantExceptions",TestStatus.FAIL);
        final TestEventType signalName = TestEventType.AUTOMATION;
        final String exceptionType1 = "org.openqa.selenium.NoSuchElementException";
        final String exceptionType2 = "org.openqa.selenium.StaleElementException";
        final String exceptionType3 = "java.lang.NullPointerException";
        final String format = "eventno:%d,type:Exception,timestamp:1639603525599 ms,cmd:findElementByWebDriver,param1:Exception Type: %s,...";
        
		testExecution.testSignals.add(createTestSignal(signalName, String.format(format,10, exceptionType1)));
		testExecution.testSignals.add(createTestSignal(signalName, String.format(format,10, exceptionType1)));
		testExecution.testSignals.add(createTestSignal(signalName, String.format(format,10, exceptionType1)));
		testExecution.testSignals.add(createTestSignal(signalName, String.format(format,12, exceptionType1)));
		testExecution.testSignals.add(createTestSignal(signalName, String.format(format,12, exceptionType2)));
		testExecution.testSignals.add(createTestSignal(signalName, String.format(format,14, exceptionType3)));
        testRunSignal.testExecutions.add(testExecution);

        return testRunSignal;
    }

    public static TestExecution createTestExecution(String name, TestStatus status){
        Instant now = Instant.now();
        TestExecution testExecution = new TestExecution();
        testExecution.startTime = now;
        testExecution.endTime = now.plusSeconds(5);
        testExecution.status = status;
        testExecution.testCaseName = name;
        testExecution.testSignals = new ArrayList<>();
        testExecution.similarity = 50;
        return testExecution;
    }

    public static TestSignal createTestSignal(TestEventType signalName, String signalValue){
        Instant now = Instant.now();

        TestSignal signal = new TestSignal();
        signal = new TestSignal();
        signal.signalName = signalName;
        signal.signalValue = signalValue;
        signal.signalTime = now.plusSeconds(2);
        signal.errorMessage = "PreDefined";
        signal.baselineScreenshotRecorderNumber = 1;
        signal.screenshotRecorderNumber = 1;
        signal.previousSignalTime = signal.signalTime.minusSeconds(5);
        signal.locatorHash = "locator";
        signal.screenshotDiffRatio = 5;
        signal.seleniumCmd = "click";
        signal.screenshotDiffAreas = new ArrayList<>();
        signal.screenshotDiffAreas.add(new Rectangle(0, 0, 100, 100));

        return signal;
    }

    public static TestAdvisorResult createTestAdvisorResult() throws IOException{
        TestAdvisorResult testAdvisorResult = new TestAdvisorResult();
        testAdvisorResult.setVersion("1.0.0");
        testAdvisorResult.setBuildStartTime(Instant.now());
        testAdvisorResult.setBuildEndTime(testAdvisorResult.getBuildStartTime().plusSeconds(500));

        TestCaseExecution testCaseExecution = new TestCaseExecution();
        testCaseExecution.setTestName("testcasePass");
        testCaseExecution.setBrowser("chrome");
        testCaseExecution.setBrowserVersion("89");
        testCaseExecution.setStartTime(Instant.now());
        testCaseExecution.setEndTime(testCaseExecution.getStartTime().plusSeconds(5));
        testCaseExecution.setTestStatus(com.salesforce.cte.common.TestStatus.PASSED);
        testCaseExecution.setScreenResolution("1920*1080");

        testAdvisorResult.getTestCaseExecutionList().add(testCaseExecution);

        return testAdvisorResult;

    }

    public static void saveTestAdvisorResult(Path testrun, TestAdvisorResult result) throws JsonGenerationException, JsonMappingException, IOException{
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
							.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		ObjectWriter objectWriter = objectMapper.writer(new DefaultPrettyPrinter());

        Path outputFilePath = testrun.resolve("test-result.json");
	    objectWriter.withDefaultPrettyPrinter().writeValue(outputFilePath.toFile(), result);
    }    

}
