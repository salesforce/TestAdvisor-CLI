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
        
        assertEquals(1, testRunSignal.testExecutions.size());
        assertEquals(TestStatus.PASS, testRunSignal.testExecutions.get(0).status);

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


    private TestCaseBase createTeseCase(int secOffset, int num, String seleniumCmd) throws IOException{
        List<TestAdvisorTestSignal> signalList = new ArrayList<>();

        signalList.add(new TestSignalBase("signal name 1", "SEVERE", Instant.now().minusSeconds(1000), 
                                            Level.SEVERE.toString(), "", "", "", 
                                            num, "" ));
        signalList.add(new TestSignalBase("signal name 2", "WARNING", Instant.now().minusSeconds(900), 
                                            Level.WARNING.toString(), "", "", "", 
                                            num, "" ));                  
        signalList.add(new TestSignalBase("signal name 3", "INFO", Instant.now().minusSeconds(800), 
                                            Level.INFO.toString(), "", "", "", 
                                            num, "" ));                    
        signalList.add(new TestSignalBase("signal name 4", "selenium signal value", Instant.now().minusSeconds(700), 
                                            Level.INFO.toString(), seleniumCmd, "slenium param", "selenium locaor1", 
                                            num, createSreenshot("test").toString()));
        signalList.add(new TestSignalBase("signal name 5", "selenium signal value", Instant.now().minusSeconds(600), 
                                            Level.INFO.toString(), seleniumCmd, "slenium param", "selenium locaor2", 
                                            num, createSreenshot("test").toString()));

        signalList.add(new TestSignalBase("signal name 6 ", "SEVERE", Instant.now().minusSeconds(500), 
                                            Level.SEVERE.toString(), "", "", "", 
                                            num, "" ));
        signalList.add(new TestSignalBase("signal name 7", "WARNING", Instant.now().minusSeconds(400), 
                                            Level.WARNING.toString(), "", "", "", 
                                            num, "" ));                  
        signalList.add(new TestSignalBase("signal name 8", "INFO", Instant.now().minusSeconds(300), 
                                            Level.INFO.toString(), "", "", "", 
                                            num, "" ));        

        TestCaseBase testcase = new TestCaseBase("TestCase",Instant.now().minusSeconds(secOffset), 
            Instant.now(),com.salesforce.cte.common.TestStatus.PASSED.toString(),false, signalList);

        return testcase;
    }

    public TestAdvisorResult createTestAdvisorResult() throws IOException{
        TestAdvisorResult testAdvisorResult = new TestAdvisorResult();
        testAdvisorResult.version = "1.0.0";
        testAdvisorResult.buildStartTime = Instant.now();
        testAdvisorResult.buildEndTime = testAdvisorResult.buildStartTime.plusSeconds(500);
        testAdvisorResult.testCaseExecutionList = new ArrayList<>();

        TestCaseExecution testCaseExecution = new TestCaseExecution("testcasePass");
        testCaseExecution.browser = "chrome";
        testCaseExecution.browserVersion = "89";
        testCaseExecution.startTime = Instant.now();
        testCaseExecution.endTime = testCaseExecution.startTime.plusSeconds(50);
        testCaseExecution.testStatus = com.salesforce.cte.common.TestStatus.PASSED;
        testCaseExecution.screenResolution = "1920*1080";
        testCaseExecution.eventList.add(new TestEvent("eventContent1", Level.SEVERE.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent2", Level.WARNING.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent3", Level.INFO.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent4", Level.INFO.toString(), "seleniumCommand1", 
                    "seleniumParam", "locator1", 1, createSreenshot("test").toFile()));
        testCaseExecution.eventList.add(new TestEvent("eventContent5", Level.SEVERE.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent6", Level.WARNING.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent7", Level.INFO.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent", Level.INFO.toString(), "seleniumCommand2", 
                    "seleniumParam", "locator2", 1, createSreenshot("test").toFile()));
        testCaseExecution.eventList.add(new TestEvent("eventContent8", Level.SEVERE.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent9", Level.WARNING.toString()));
        testCaseExecution.eventList.add(new TestEvent("eventContent10", Level.INFO.toString()));

        testAdvisorResult.testCaseExecutionList.add(testCaseExecution);

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