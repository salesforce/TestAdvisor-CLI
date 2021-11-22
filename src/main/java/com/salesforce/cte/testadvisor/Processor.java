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
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.salesforce.cte.adapter.TestAdvisorAdapter;
import com.salesforce.cte.adapter.TestAdvisorResultAdapter;
import com.salesforce.cte.adapter.TestAdvisorTestCase;
import com.salesforce.cte.adapter.TestAdvisorTestRun;
import com.salesforce.cte.adapter.TestAdvisorTestSignal;
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

    private static final Logger LOGGER = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    
    private Registry registry;

    public Processor(Registry registry){
        this.registry = registry;
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
     */
    public void process(InputStream inputStream, TestRunSignal testRunSignal,TestAdvisorAdapter adapter) 
                            throws ProcessException, IOException{
        TestAdvisorTestRun testRun = adapter.process(inputStream);
        testRunSignal.buildStartTime = testRun.getTestSuiteStartTime() == null ? null : testRun.getTestSuiteStartTime();
        testRunSignal.buildEndTime = testRun.getTestSuiteEndTime() == null ? null : testRun.getTestSuiteEndTime();
        testRunSignal.clientLibraryVersion = testRun.getTestAdvisorVersion();
        testRunSignal.testSuiteName = testRunSignal.testSuiteName.isEmpty() ? testRun.getTestSuiteName() : testRunSignal.testSuiteName;
        testRunSignal.clientBuildId = testRunSignal.clientBuildId.isEmpty() ? testRun.getTestsSuiteInfo() : testRunSignal.clientBuildId;
        testRunSignal.testExecutions = new ArrayList<>();
        for(TestAdvisorTestCase testCase : testRun.getTestCaseList()){
            LOGGER.info("Processing test case:"+testCase.getTestCaseFullName());
            Path baseline = registry.getBaselineTestRun(registry.getTestRunPath(testRunSignal.testRunId), testCase.getTestCaseFullName());
            TestExecution testExection = new TestExecution();
            testExection.baselineBuildId = registry.getTestRunId(baseline);
            testExection.baselineBuildIdStartTime = getTestRunStartTime(baseline);
            testExection.baselineSalesforceBuildId = getSalesforceId(baseline);
            testExection.testCaseName = testCase.getTestCaseFullName();
            testExection.startTime = testCase.getTestCaseStartTime();
            testExection.endTime = testCase.getTestCaseEndTime();
            testExection.status = enumPartialMatch(TestStatus.class, testCase.getTestCaseStatus());
            testExection.testSignals = new ArrayList<>();
            TestAdvisorTestCase baselineCase = getTestCaseFromTestRun(baseline,testCase.getTestCaseFullName());
            testExection.similarity = compareTestCaseExecution(baselineCase, testCase, testExection.testSignals);
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
        if (baseline == null) return 0;
        signalList.clear();
        // sort event list by event time, oldest first
        Comparator<TestAdvisorTestSignal> compareByEventTime = Comparator.comparing(TestAdvisorTestSignal::getTestSignalTime);
        List<TestAdvisorTestSignal> baselineEventList = baseline.getTestSignalList();
        baselineEventList.sort(compareByEventTime);
        List<TestAdvisorTestSignal> currentEventList =  current.getTestSignalList();
        currentEventList.sort(compareByEventTime);

        // get test step list by unique screenshots
        List<TestAdvisorTestSignal> baselineSteps = getTestStepListByUniqueScreenshots(baselineEventList);
        List<TestAdvisorTestSignal> currentSteps = getTestStepListByUniqueScreenshots(currentEventList);

        int i=0; //current test step index
        int j=0; //baseline test step index
        int matchCount=0;
        
        List<TestSignal> subSignalList = new ArrayList<>();
        TestAdvisorTestSignal prevStep = null;
        //for every event in current test
        for(TestAdvisorTestSignal event : currentEventList){
            if (i>=currentSteps.size() 
                && (event.getTestSignalLevel().equals("SEVERE") || event.getTestSignalLevel().equals("WARNING"))){
                //finished all current test steps
                //only extract WARNING or SEVERE event for rest events
                signalList.add(createTestSignalFromEvent(event));
            }
            if (i>=currentSteps.size())
                continue;
            TestAdvisorTestSignal currentStep = currentSteps.get(i);

            if (event == currentStep){
                //current event is a test step
                //try to find a match baseline steps       
                while(j<baselineSteps.size() && !isMatchScreenshotEvent(currentStep, baselineSteps.get(j))){
                    j++;
                }
                     
                if(j<baselineSteps.size()){
                    // find a match baseline step
                    TestAdvisorTestSignal baselineStep = baselineSteps.get(j);
                    matchCount++;
                    // image comparison
                    LOGGER.info("Comparing images, baseline:"+baselineStep.getTestSignalScreenshotPath() 
                            + " current:"+currentStep.getTestSignalScreenshotPath());
                    Path currentPath = Paths.get(currentStep.getTestSignalScreenshotPath());
                    String resultFile = currentPath.getParent().resolve(currentPath.getFileName().toString()+"compareresult.png").toString();
                    ImageComparisonResult result = ScreenshotManager.screenshotsComparison(
                        new File(baselineStep.getTestSignalScreenshotPath()),new File(currentStep.getTestSignalScreenshotPath()),resultFile);
                    
                    if (result.getImageComparisonState() == ImageComparisonState.MISMATCH 
                        && result.getDifferencePercent() > 25.0f ){
                        //image comparison found diff
                        TestSignal signal = createTestSignalFromEvent(event);
                        signal.subSignalList = subSignalList;
                        signal.screenshotDiffRatio = (int) result.getDifferencePercent();
                        signal.baselinScreenshotRecorderNumber = baselineStep.getTestSignalScreenshotRecorderNumber();
                        signal.screenshotDiffAreas = result.getRectangles();
                        signal.previousSignalTime = prevStep == null ?  null : prevStep.getTestSignalTime();
                        signalList.add(signal);
                    }
                    prevStep = currentStep;
                    subSignalList = new ArrayList<>();
                    i++;
                    j++;
                }
            }
            else{
                //add all event between last test step and current step to sub signal list
                if ((event.getTestSignalLevel().equals("SEVERE") || event.getTestSignalLevel().equals("WARNING")))
                    subSignalList.add(createTestSignalFromEvent(event));
            }
        }

        return  (int)(((float)matchCount)/currentSteps.size() * 100);
    }

    private TestSignal createTestSignalFromEvent(TestAdvisorTestSignal event){
        TestSignal signal = new TestSignal();
        signal.signalName = event.getTestSignalName();
        signal.signalValue = event.getTestSignalValue();
        signal.signalTime = event.getTestSignalTime();
        signal.screenshotRecorderNumber = event.getTestSignalScreenshotRecorderNumber();
        signal.locator = getMD5Hash(event.getTestSignalSeleniumLocator());
        signal.locator = getMD5Hash(signal.locator);
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

    private String getMD5Hash(String s) {
        if (s==null || s.isEmpty()) 
            return "";

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
        messageDigest.update(s.getBytes());
        return new String(messageDigest.digest());
    }

    private TestAdvisorTestCase getTestCaseFromTestRun(Path testRun, String testCaseName) throws IOException, ProcessException{
        TestAdvisorTestRun advisorRun;
        if (testRun == null) return null;
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
        try(InputStream is = new FileInputStream(testRun.resolve(Registry.TESTADVISOR_TEST_RESULT).toFile())){
            advisorRun = new TestAdvisorResultAdapter().process(is);
        }

        return advisorRun.getTestSuiteStartTime();
    }

    private String getSalesforceId(Path testRun) throws JSONException, FileNotFoundException{
        if (testRun==null) return "";
        JSONObject jsonObject = (JSONObject) new JSONTokener(new FileReader(
            testRun.resolve(Registry.PORTAL_RECORD_FILENAME).toFile())).nextValue();
		return jsonObject.getString("Id");
    }

}
