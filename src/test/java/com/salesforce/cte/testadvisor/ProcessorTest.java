/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Color;

import com.salesforce.cte.adapter.TestAdvisorAdapter;
import com.salesforce.cte.adapter.TestAdvisorResultAdapter;
import com.salesforce.cte.adapter.TestAdvisorTestCase;
import com.salesforce.cte.adapter.TestAdvisorTestSignal;
import com.salesforce.cte.adapter.TestCaseBase;
import com.salesforce.cte.adapter.TestNGAdapter;
import com.salesforce.cte.adapter.TestSignalBase;
import com.salesforce.cte.common.TestAdvisorResult;
import com.salesforce.cte.common.TestCaseExecution;
import com.salesforce.cte.common.TestEvent;
import com.salesforce.cte.common.TestEventType;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestSignal;
import com.salesforce.cte.datamodel.client.TestStatus;
import com.salesforce.cte.helper.ProcessException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessorTest{

    private Path root;
    private Registry registry;
    private Processor processor;
    private int screenshotRecorderNumber=0;

    @Before
    public void setup() throws IOException {
        root = Files.createTempDirectory("testadvisor");
        registry = new Registry(root);
        processor = new Processor(registry);
    }

    @Test
    public void testProcessTestNGXml() throws IOException, ProcessException{
        TestRunSignal testRunSignal = new TestRunSignal();
        testRunSignal.clientBuildId = "123";
        testRunSignal.clientCliVersion = "1.0.1";
        testRunSignal.clientLibraryVersion = "1.0.1";
        testRunSignal.clientRegistryGuid = UUID.randomUUID();
        testRunSignal.sandboxInstance = "CS997";
        testRunSignal.sandboxOrgId = "00D9A0000009IsD";
        testRunSignal.sandboxOrgName = "bst";
        testRunSignal.testSuiteName = "testSuite1";
        TestAdvisorAdapter adapter = new TestNGAdapter();
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("xml/testng-results.xml")){
            processor.process(is, testRunSignal,adapter);
        }
        
        assertEquals(32, testRunSignal.testExecutions.size());
    }

    @Test
    public void testProcessPredefinedTestAdvisorResult() throws IOException, ProcessException{
        TestRunSignal testRunSignal = new TestRunSignal();
        testRunSignal.clientBuildId = "123";
        testRunSignal.clientCliVersion = "1.0.1";
        testRunSignal.clientRegistryGuid = UUID.randomUUID();
        testRunSignal.sandboxInstance = "CS997";
        testRunSignal.sandboxOrgId = "00D9A0000009IsD";
        testRunSignal.sandboxOrgName = "bst";
        testRunSignal.testSuiteName = "testSuite1";
        TestAdvisorAdapter adapter = new TestAdvisorResultAdapter();
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("json/test-result.json")){
            processor.process(is, testRunSignal,adapter);
        }
        
        assertEquals(40, testRunSignal.testExecutions.size());
        assertEquals(TestStatus.PASS, testRunSignal.testExecutions.get(0).status);
        assertEquals(TestStatus.PASS, testRunSignal.testExecutions.get(1).status);
        assertEquals("1.1.1", testRunSignal.clientLibraryVersion);
        assertEquals("1.0.1", testRunSignal.clientCliVersion);
    }

    @Test
    public void testProcessTestAdvisorResult() throws IOException, ProcessException{
        
        Path currentTestRun = RegistryHelper.createTestRun(registry, 1000);
        RegistryHelper.saveTestAdvisorResult(currentTestRun, createTestAdvisorResult());
        Path baselineTestRun = RegistryHelper.createTestRun(registry, 700);
        RegistryHelper.saveTestAdvisorResult(baselineTestRun, createTestAdvisorResult());
        Path controlTestRun = RegistryHelper.createTestRun(registry, 400);
        RegistryHelper.saveTestAdvisorResult(controlTestRun, createTestAdvisorResult());
        registry.getAllTestRuns();
        
        TestRunSignal testRunSignal = new TestRunSignal();
        testRunSignal.clientBuildId = "123";
        testRunSignal.clientCliVersion = "1.0.1";
        testRunSignal.clientRegistryGuid = UUID.randomUUID();
        testRunSignal.sandboxInstance = "CS997";
        testRunSignal.sandboxOrgId = "00D9A0000009IsD";
        testRunSignal.sandboxOrgName = "bst";
        testRunSignal.testSuiteName = "testSuite1";
        testRunSignal.testRunId = registry.getTestRunId(currentTestRun);
        TestAdvisorAdapter adapter = new TestAdvisorResultAdapter();
        try(InputStream is = new FileInputStream(currentTestRun.resolve("test-result.json").toFile())){
            processor.process(is, testRunSignal,adapter);
        }
        
        assertEquals(2, testRunSignal.testExecutions.size());
        assertEquals(TestStatus.PASS, testRunSignal.testExecutions.get(0).status);
    }

    @Test
    public void testUploadAllConfTest() throws IOException, ProcessException{
        Path currentTestRun = RegistryHelper.createTestRun(registry, 1000);
        RegistryHelper.saveTestAdvisorResult(currentTestRun, createTestAdvisorResult());
        registry.getAllTestRuns();

        TestRunSignal testRunSignal = registry.getTestRunProperties();
        TestAdvisorAdapter adapter = new TestAdvisorResultAdapter();
        try(InputStream is = new FileInputStream(currentTestRun.resolve("test-result.json").toFile())){
            processor.process(is, testRunSignal,adapter);
        }

        assertEquals(2, testRunSignal.testExecutions.size());

        System.setProperty("testadvisor.uploadallconfigurationtest","true");
        testRunSignal = registry.getTestRunProperties();
        try(InputStream is = new FileInputStream(currentTestRun.resolve("test-result.json").toFile())){
            processor.process(is, testRunSignal,adapter);
        }
        assertEquals(3, testRunSignal.testExecutions.size());
        System.setProperty("testadvisor.uploadallconfigurationtest","false");

    }

    @Test
    public void testCompareSameTestCase() throws IOException{
        TestAdvisorTestCase baseline = createTeseCase(100, 100, "selcmd");
        TestAdvisorTestCase current = createTeseCase(50, 200, "selcmd");
        List<TestSignal> signalList = new ArrayList<>();

        System.setProperty("testadvisor.screenshotmindiffareasize","9");
        assertEquals(100, processor.compareTestCaseExecution(baseline, current, signalList));
        assertEquals(6, signalList.size());
    }

    @Test
    public void testCompareDiffTestCase() throws IOException{
        TestAdvisorTestCase baseline = createTeseCase(100, 100, "selcmd1");
        TestAdvisorTestCase current = createTeseCase(50, 200, "selcmd2");
        List<TestSignal> signalList = new ArrayList<>();

        System.setProperty("testadvisor.screenshotcomparison","true");
        assertEquals(0, processor.compareTestCaseExecution(baseline, current, signalList));
        assertEquals(4, signalList.size());
    }

    @Test
    public void testScreenshotCompare() throws IOException{
        TestAdvisorTestCase baseline = createTeseCase(100, 100, "selcmd1");
        TestAdvisorTestCase current = createTeseCase(50, 200, "selcmd2");
        List<TestSignal> signalList = new ArrayList<>();

        System.setProperty("testadvisor.screenshotcomparison","true");
        System.setProperty("testadvisor.exportscreenshotdiffimage","true");
        System.setProperty("testadvisor.screenshotmindiffareasize","0");
        System.setProperty("testadvisor.screenshotmindiffratio","0");

        assertEquals(0, processor.compareTestCaseExecution(baseline, current, signalList));
    }

    @Test
    public void testSeleniumUrl() throws IOException{
        TestAdvisorTestCase current = createTeseCase(50, 200, "selcmd2");
        List<TestSignal> signalList = new ArrayList<>();
        System.setProperty("testadvisor.selenium.url","true");
        System.setProperty("testadvisor.signallevel","OFF");
        processor.extractTestSignals(current, signalList);
        assertEquals(1, signalList.size());
        assertEquals(TestEventType.URL, signalList.get(0).signalName); 
        System.setProperty("testadvisor.signallevel","WARNING");
        System.setProperty("testadvisor.selenium.url","false");
    }

    @Test
    public void testSeleniumException() throws IOException{
        TestAdvisorTestCase current = createTeseCase(50, 200, "selcmd2");
        List<TestSignal> signalList = new ArrayList<>();
        System.setProperty("testadvisor.selenium.exception","true");
        System.setProperty("testadvisor.signallevel","OFF");
        processor.extractTestSignals(current, signalList);
        assertEquals(1, signalList.size());
        assertEquals(TestEventType.TEST_EXCEPTION, signalList.get(0).signalName); 
        System.setProperty("testadvisor.signallevel","WARNING");
        System.setProperty("testadvisor.selenium.exception","false");
    }

    private TestCaseBase createTeseCase(int secOffset, int num, String seleniumCmd) throws IOException{
        List<TestAdvisorTestSignal> signalList = new ArrayList<>();

        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "SEVERE", Instant.now().minusSeconds(1000), 
                                            Level.SEVERE.toString(), "", "", "", 
                                            num, "" ));
        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "WARNING", Instant.now().minusSeconds(900), 
                                            Level.WARNING.toString(), "", "", "", 
                                            num, "" ));                  
        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "INFO", Instant.now().minusSeconds(800), 
                                            Level.INFO.toString(), "", "", "", 
                                            num, "" ));                    
        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "selenium signal value", Instant.now().minusSeconds(700), 
                                            Level.INFO.toString(), seleniumCmd, "slenium param", "selenium locaor1", 
                                            num, createSreenshot("test").toString()));
        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "selenium signal value", Instant.now().minusSeconds(600), 
                                            Level.INFO.toString(), seleniumCmd, "slenium param", "selenium locaor2", 
                                            num, createSreenshot("test").toString()));

        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "SEVERE", Instant.now().minusSeconds(500), 
                                            Level.SEVERE.toString(), "", "", "", 
                                            num, "" ));
        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "WARNING", Instant.now().minusSeconds(400), 
                                            Level.WARNING.toString(), "", "", "", 
                                            num, "" ));                  
        signalList.add(new TestSignalBase(TestEventType.AUTOMATION, "INFO", Instant.now().minusSeconds(300), 
                                            Level.INFO.toString(), "", "", "", 
                                            num, "" ));     
        signalList.add(new TestSignalBase(TestEventType.URL, "url", Instant.now().minusSeconds(300), 
                                            Level.INFO.toString(), "", "", "", 
                                            num, "" ));                                                    
        signalList.add(new TestSignalBase(TestEventType.TEST_EXCEPTION, "exception", Instant.now().minusSeconds(300), 
                                            Level.INFO.toString(), "", "", "", 
                                            num, "" ));     

        TestCaseBase testcase = new TestCaseBase("TestCase",Instant.now().minusSeconds(secOffset), 
            Instant.now(),com.salesforce.cte.common.TestStatus.PASSED.toString(),false, 0, signalList);

        return testcase;
    }

    public TestAdvisorResult createTestAdvisorResult() throws IOException{
        TestAdvisorResult testAdvisorResult = new TestAdvisorResult();
        testAdvisorResult.setVersion("1.0.0");
        testAdvisorResult.setBuildStartTime(Instant.now());
        testAdvisorResult.setBuildEndTime(testAdvisorResult.getBuildStartTime().plusSeconds(500));


        TestCaseExecution testCaseExecution = new TestCaseExecution();
        testCaseExecution.setTestName("testcasePass");
        testCaseExecution.setBrowser("chrome");
        testCaseExecution.setBrowserVersion("89");
        testCaseExecution.setStartTime(Instant.now());
        testCaseExecution.setEndTime(testCaseExecution.getStartTime().plusSeconds(50));
        testCaseExecution.setTestStatus(com.salesforce.cte.common.TestStatus.PASSED);
        testCaseExecution.setScreenResolution("1920*1080");
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent1", Level.SEVERE.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent2", Level.WARNING.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent3", Level.INFO.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent4", Level.INFO.toString(), "seleniumCommand1", 
                    "seleniumParam", "locator1", 1, createSreenshot("test").toFile()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent5", Level.SEVERE.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent6", Level.WARNING.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent7", Level.INFO.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent", Level.INFO.toString(), "seleniumCommand2", 
                    "seleniumParam", "locator2", 1, createSreenshot("test").toFile()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent8", Level.SEVERE.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent9", Level.WARNING.toString()));
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent10", Level.INFO.toString()));

        testAdvisorResult.getTestCaseExecutionList().add(testCaseExecution);

        testCaseExecution = new TestCaseExecution();
        testCaseExecution.setTestName("testcaseSetup");
        testCaseExecution.setConfiguration(true);
        testCaseExecution.setStartTime(Instant.now());
        testCaseExecution.setEndTime(testCaseExecution.getStartTime().plusSeconds(50));
        testCaseExecution.setTestStatus(com.salesforce.cte.common.TestStatus.PASSED);
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent10", Level.INFO.toString()));
        testAdvisorResult.getTestCaseExecutionList().add(testCaseExecution);

        testCaseExecution = new TestCaseExecution();
        testCaseExecution.setTestName("testcaseSetup");
        testCaseExecution.setConfiguration(true);
        testCaseExecution.setStartTime(Instant.now());
        testCaseExecution.setEndTime(testCaseExecution.getStartTime().plusSeconds(50));
        testCaseExecution.setTestStatus(com.salesforce.cte.common.TestStatus.FAILED);
        testCaseExecution.getEventList().add(new TestEvent(TestEventType.AUTOMATION,"eventContent10", Level.INFO.toString()));
        testAdvisorResult.getTestCaseExecutionList().add(testCaseExecution);

        return testAdvisorResult;
    }

    private Path createSreenshot(String text) throws IOException{
        Path screenshotPath = root.resolve("Screenshots");
        screenshotPath.toFile().mkdirs();
        screenshotPath = screenshotPath.resolve(String.valueOf(++screenshotRecorderNumber)+".png");
        
        Files.copy(getClass().getClassLoader().getResourceAsStream("image/login.png"), screenshotPath);
        BufferedImage image = ImageIO.read(screenshotPath.toFile());
        Font font = new Font("Arial", Font.BOLD, 18);

        Graphics g = image.getGraphics();
        g.setFont(font);
        g.setColor(Color.RED);
        g.drawString(text+screenshotRecorderNumber, 20, 20);

        ImageIO.write(image, "png", screenshotPath.toFile());

        return screenshotPath;
    }

    @After
    public void teardown() throws IOException{
        removeDirectory(root.toFile());
    }

    private void removeDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }

}