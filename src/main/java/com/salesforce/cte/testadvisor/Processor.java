/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.github.romankh3.image.comparison.model.Rectangle;
import com.salesforce.cte.adapter.TestAdvisorAdapter;
import com.salesforce.cte.adapter.TestAdvisorResultAdapter;
import com.salesforce.cte.adapter.TestAdvisorTestCase;
import com.salesforce.cte.adapter.TestAdvisorTestRun;
import com.salesforce.cte.adapter.TestAdvisorTestSignal;
import com.salesforce.cte.common.TestEventType;
import com.salesforce.cte.datamodel.client.TestExecution;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestSignal;
import com.salesforce.cte.datamodel.client.TestStatus;
import com.salesforce.cte.helper.ProcessException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author Yibing Tao
 * This class provide method to process test result with the provided adapter
 */
public class Processor {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private Registry registry;
    private ScreenshotManager screenshotManager;

    public Processor(Registry registry){
        this.registry = registry;
        this.screenshotManager = new ScreenshotManager();
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
     * @throws IOException
     * throws this exception when fail to access test run files
     */
    public void process(InputStream inputStream, TestRunSignal testRunSignal,TestAdvisorAdapter adapter) 
                            throws ProcessException, IOException{
        TestAdvisorTestRun testRun = adapter.process(inputStream);
        testRunSignal.buildStartTime = testRun.getTestSuiteStartTime();
        testRunSignal.buildEndTime = testRun.getTestSuiteEndTime();
        testRunSignal.clientLibraryVersion = testRun.getTestAdvisorVersion();
        testRunSignal.testSuiteName = testRunSignal.testSuiteName.isEmpty() ? testRun.getTestSuiteName() : testRunSignal.testSuiteName;
        testRunSignal.clientBuildId = testRunSignal.clientBuildId.isEmpty() ? testRun.getTestsSuiteInfo() : testRunSignal.clientBuildId;
        testRunSignal.testExecutions = new ArrayList<>();
        for(TestAdvisorTestCase testCase : testRun.getTestCaseList()){
            LOGGER.log(Level.INFO,"Processing test case {0}", testCase.getTestCaseFullName());
            //find baseline test run
            TestExecution testExection = new TestExecution();
            testExection.testCaseName = testCase.getTestCaseFullName();
            testExection.startTime = testCase.getTestCaseStartTime();
            testExection.endTime = testCase.getTestCaseEndTime();
            testExection.status = enumPartialMatch(TestStatus.class, testCase.getTestCaseStatus());
            testExection.isConfiguration = testCase.getIsConfiguration();
            testExection.traceId = testCase.getTraceId();
            testExection.testSignals = new ArrayList<>();

            Path baseline = registry.getBaselineTestRun(registry.getTestRunPath(testRunSignal.testRunId), testCase.getTestCaseFullName());
            if (baseline != null && Configuration.getIsSeleniumScreenshotEnabled()){
                LOGGER.log(Level.INFO,"Found baseline {0}", baseline);
                testExection.baselineBuildId = registry.getTestRunId(baseline);
                testExection.baselineBuildIdStartTime = getTestRunStartTime(baseline);
                testExection.baselineSalesforceBuildId = getSalesforceId(baseline);
                //find baseline test case
                TestAdvisorTestCase baselineCase = getTestCaseFromTestRun(baseline,testCase.getTestCaseFullName());
                
                //try to find excluded areas for baseline test case
                setExcludedAreas(baseline,testCase);
                //find similarity and extract signals
                testExection.similarity = compareTestCaseExecution(baselineCase, testCase, testExection.testSignals);
            }
            else   
                extractTestSignals(testCase,testExection.testSignals);

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

    public void extractTestSignals(TestAdvisorTestCase current, List<TestSignal> signalList){
        current.getTestSignalList().stream().filter(signal ->  
            (Configuration.getIsSeleniumExceptionEnabled() && signal.getTestSignalName() == TestEventType.EXCEPTION) 
            || (Configuration.getIsSeleniumUrlEnabled() && signal.getTestSignalName() == TestEventType.URL)
            || (signal.getTestSignalName() == TestEventType.AUTOMATION) && signal.getTestSignalLevel().intValue() >= Configuration.getSignalLevel().intValue())
            .forEach(signal -> signalList.add(createTestSignalFromEvent(signal))); 
    }

    /**
     * Compare test advisor test case execution result based on screenshots and collect signals
     * @param baseline baseline test case execution
     * @param current current test case execution
     * @param signalList output signal list contains 
     * @return
     * similarity between baseline and current test case execution
     * similarity = match test steps count / current test case steps count (0 - 100)
     */
    public int compareTestCaseExecution(TestAdvisorTestCase baseline, TestAdvisorTestCase current, List<TestSignal> signalList){
        LOGGER.info("Start compareTestCaseExecution");
        signalList.clear();
        // sort event list by event time, oldest first
        Comparator<TestAdvisorTestSignal> compareByEventTime = Comparator.comparing(TestAdvisorTestSignal::getTestSignalTime);
        List<TestAdvisorTestSignal> baselineEventList = baseline != null ? baseline.getTestSignalList() : new ArrayList<>();
        baselineEventList.sort(compareByEventTime);
        List<TestAdvisorTestSignal> currentEventList =  current.getTestSignalList();
        currentEventList.sort(compareByEventTime);

        // get test step list by unique screenshots
        List<TestAdvisorTestSignal> baselineSteps = getTestStepListByUniqueScreenshots(baselineEventList);
        List<TestAdvisorTestSignal> currentSteps = getTestStepListByUniqueScreenshots(currentEventList);
        LOGGER.log(Level.INFO, "baselineSteps count:{0}",baselineSteps.size());
        LOGGER.log(Level.INFO, "currentSteps count {0}",currentSteps.size());
    

        int i=0; //current test step index
        int j=0; //baseline test step index
        int matchCount=0;
        
        TestAdvisorTestSignal prevStep = null;
        //for every event in current test
        for(TestAdvisorTestSignal event : currentEventList){         
            if (i>=currentSteps.size() || event != currentSteps.get(i)) {
                //current is NOT a test step
                if (event.getTestSignalLevel().intValue() >= Configuration.getSignalLevel().intValue()){
                    signalList.add(createTestSignalFromEvent(event));
                }
                continue;
            }

            //current event is a test step 
            //try to find a match baseline steps  
            TestAdvisorTestSignal currentStep = currentSteps.get(i);     
            while(j<baselineSteps.size() && !isMatchScreenshotEvent(currentStep, baselineSteps.get(j))){
                j++;
            }

            if(j<baselineSteps.size() && fileExist(currentStep.getTestSignalScreenshotPath())
                && fileExist(baselineSteps.get(j).getTestSignalScreenshotPath())){
                // find a match baseline step
                TestAdvisorTestSignal baselineStep = baselineSteps.get(j);
                // image comparison
                Path currentPath = Paths.get(currentStep.getTestSignalScreenshotPath());

                ImageComparisonResult result;
                if (Configuration.getExportScreenshotDiffImage()){
                    File resultFile = currentPath.getParent().resolve(currentPath.getFileName().toString()+".compareresult.png").toFile();
                    result = screenshotManager.screenshotsComparisonWithExcludedAreas(
                        new File(baselineStep.getTestSignalScreenshotPath()),new File(currentStep.getTestSignalScreenshotPath()),resultFile
                    ,currentStep.getExcludedAreas());
                }else{
                    result = screenshotManager.screenshotsComparisonWithExcludedAreas(
                        new File(baselineStep.getTestSignalScreenshotPath()),new File(currentStep.getTestSignalScreenshotPath())
                    ,currentStep.getExcludedAreas());
                }

                if (result.getImageComparisonState() == ImageComparisonState.MISMATCH 
                    && isDiffAreaLargeThanThreshhold(result)){
                    //image comparison found diff
                    TestSignal signal = createTestSignalFromEvent(event);
                    signal.screenshotDiffRatio = getDiffRatio(result); 
                    LOGGER.log(Level.INFO, "Found diff from screenshot comparison, ratio:{0}",signal.screenshotDiffRatio);
                    signal.baselineScreenshotRecorderNumber = baselineStep.getTestSignalScreenshotRecorderNumber();
                    if (Configuration.getExportScreenshotDiffArea())
                        signal.screenshotDiffAreas = result.getRectangles();
                    signal.previousSignalTime = prevStep == null ?  current.getTestCaseStartTime() : prevStep.getTestSignalTime();
                    signalList.add(signal);
                }
                matchCount++;
                j++;
            }
            prevStep = currentStep;
            i++;
        }

        return  (int)(((float)matchCount)/currentSteps.size() * 100);
    }

    private int getDiffRatio(ImageComparisonResult result){
        long imageSize = (long) result.getActual().getWidth() * result.getActual().getHeight();
        long diffSize = 0;
        for(Rectangle rect : result.getRectangles()){
            diffSize += (long)rect.getHeight() * rect.getWidth();
        }
        return imageSize > 0 ? (int) (diffSize * 100 / (float)imageSize) : 0 ;
    }

    private boolean isDiffAreaLargeThanThreshhold(ImageComparisonResult result){
        boolean ret = false;
        for(Rectangle rect : result.getRectangles()){
             ret |= Math.min(rect.getHeight(),rect.getWidth()) > Configuration.getScreenshotMinDiffAreaSize();
        }

        if (Configuration.getScreenshotMinDiffRatio()>0){
            ret |= getDiffRatio(result) > Configuration.getScreenshotMinDiffRatio();
        }

        return ret;
    }

    private boolean fileExist(String filename){
        return new File(filename).exists() && new File(filename).canRead();
    }
    
    private TestSignal createTestSignalFromEvent(TestAdvisorTestSignal event){
        TestSignal signal = new TestSignal();
        signal.signalName = event.getTestSignalName();
        signal.signalValue = event.getTestSignalValue();
        signal.signalTime = event.getTestSignalTime();
        signal.screenshotRecorderNumber = event.getTestSignalScreenshotRecorderNumber();
        //signal.locator = event.getTestSignalSeleniumLocator();
        signal.locatorHash = getHash(signal.locator);
        signal.seleniumCmd = event.getTestSignalSeleniumCmd();
        return signal;
    }
    /**
     * Get list of screenshot events for event list which represent the test steps
     * add all other events as 
     * @param testEventList list of test events order by event time
     * @return list of screenshot events
     */
    private List<TestAdvisorTestSignal> getTestStepListByUniqueScreenshots(List<TestAdvisorTestSignal> testEventList){
        List<TestAdvisorTestSignal> testSteps = new ArrayList<>();
        TestAdvisorTestSignal prevStep = null;
        for(TestAdvisorTestSignal event : testEventList){
            if (event.getTestSignalScreenshotPath() == null 
                || event.getTestSignalScreenshotPath().isEmpty()
                || isMatchScreenshotEvent(event, prevStep)) continue;
            testSteps.add(event);
            prevStep=event;
        }
        return testSteps;
    }   

    /**
     * Check whether 2 screenshot event are match
     * @param event1 first event
     * @param event2 second event
     * @return
     * true if both event consider same type
     * false otherwise
     */
    private boolean isMatchScreenshotEvent(TestAdvisorTestSignal event1, TestAdvisorTestSignal event2){
        if(event1 == null || event2 == null)
             return false;
        return event1.getTestSignalSeleniumCmd().equals(event2.getTestSignalSeleniumCmd())
                //&& event1.getTestSignalSeleniumParam().equals(event2.getTestSignalSeleniumParam())
                && event1.getTestSignalSeleniumLocator().equals(event2.getTestSignalSeleniumLocator());
    }

    private String getHash(String s) {
        if (s==null || s.isEmpty()) 
            return "";

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warning(e.toString());
            return "";
        }
        messageDigest.update(s.getBytes());
        return Base64.getEncoder().encodeToString((messageDigest.digest()));
    }

    private TestAdvisorTestCase getTestCaseFromTestRun(Path testRun, String testCaseName) throws IOException, ProcessException{
        TestAdvisorTestRun advisorRun;
        if (testRun == null) return null;
        if (!testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile().exists()
            || !testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile().canRead()) return null;
        try(InputStream is = new FileInputStream(testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile())){
            advisorRun = new TestAdvisorResultAdapter().process(is);
        }

        for (TestAdvisorTestCase testcase : advisorRun.getTestCaseList()){
            if (testcase.getTestCaseFullName().equals(testCaseName))
                return testcase;
        }
        return null;
    }

    private Instant getTestRunStartTime(Path testRun) throws IOException, ProcessException{
        TestAdvisorTestRun advisorRun;
        if (testRun == null) return null;
        if (!testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile().exists()
            || !testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile().canRead()) return null;
        try(InputStream is = new FileInputStream(testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile())){
            advisorRun = new TestAdvisorResultAdapter().process(is);
        }

        return advisorRun.getTestSuiteStartTime();
    }

    private String getSalesforceId(Path testRun) throws JSONException, FileNotFoundException{
        if (testRun==null) return "";
        if (!testRun.resolve(Registry.PORTAL_RECORD_FILENAME).toFile().exists()
            || !testRun.resolve(Registry.PORTAL_RECORD_FILENAME).toFile().canRead()) return "";
        JSONObject jsonObject = (JSONObject) new JSONTokener(new FileReader(
            testRun.resolve(Registry.PORTAL_RECORD_FILENAME).toFile())).nextValue();
		return jsonObject.getString("Id");
    }

    /**
     * Try to set excluded areas for current test case from image comparison
     * excluded areas are defined as different image areas by compare last 2 success test runs
     * @param testrun
     * @param current
     * @throws IOException
     * @throws ProcessException
     */
    private void setExcludedAreas(Path testrun, TestAdvisorTestCase current) throws IOException, ProcessException{
        LOGGER.log(Level.INFO,"Start getExcludedAreas for test {0}",current.getTestCaseFullName());
        LOGGER.log(Level.INFO,"baseline test run {0}",testrun);

        Path baselineRun = registry.getBaselineTestRun(testrun, current.getTestCaseFullName());
        if (baselineRun == null) // failed to find a baseline run
            return;
        LOGGER.log(Level.INFO,"control test run {0}",baselineRun);

        TestAdvisorTestCase baseline = getTestCaseFromTestRun(baselineRun,current.getTestCaseFullName());

        // sort event list by event time, oldest first
        Comparator<TestAdvisorTestSignal> compareByEventTime = Comparator.comparing(TestAdvisorTestSignal::getTestSignalTime);
        List<TestAdvisorTestSignal> baselineEventList = baseline != null ? baseline.getTestSignalList() : new ArrayList<>();
        baselineEventList.sort(compareByEventTime);
        List<TestAdvisorTestSignal> currentEventList =  current.getTestSignalList();
        currentEventList.sort(compareByEventTime);

        // get test step list by unique screenshots
        List<TestAdvisorTestSignal> baselineSteps = getTestStepListByUniqueScreenshots(baselineEventList);
        List<TestAdvisorTestSignal> currentSteps = getTestStepListByUniqueScreenshots(currentEventList);

        int j=0; //baseline test step index
        //compare all test steps and get excluded areas
        for(TestAdvisorTestSignal currentStep : currentSteps){
                  
            while(j<baselineSteps.size() && !isMatchScreenshotEvent(currentStep, baselineSteps.get(j))){
                j++;
            }
                    
            if(j<baselineSteps.size() && fileExist(baselineSteps.get(j).getTestSignalScreenshotPath())
                && fileExist(currentStep.getTestSignalScreenshotPath())){
                LOGGER.log(Level.INFO,"current step number {0}",currentStep.getTestSignalScreenshotRecorderNumber());
                // find a match baseline step
                TestAdvisorTestSignal baselineStep = baselineSteps.get(j);
                // image comparison
                Path currentPath = Paths.get(currentStep.getTestSignalScreenshotPath());
                File resultFile = currentPath.getParent().resolve(currentPath.getFileName().toString()+".ignoredareas.png").toFile();
                ImageComparisonResult result = screenshotManager.screenshotsComparison(
                    new File(baselineStep.getTestSignalScreenshotPath()),new File(currentStep.getTestSignalScreenshotPath()),resultFile);
                if (result.getRectangles() != null){
                    LOGGER.log(Level.INFO,"exclude areas list size {0}",result.getRectangles().size());
                    currentStep.setExcludedAreas(result.getRectangles());
                }
                j++;
            }
        }
    }

}
