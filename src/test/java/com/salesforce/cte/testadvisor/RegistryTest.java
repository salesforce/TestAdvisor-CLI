package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.romankh3.image.comparison.model.Rectangle;
import com.salesforce.cte.common.TestAdvisorResult;
import com.salesforce.cte.common.TestCaseExecution;
import com.salesforce.cte.common.TestStateMapper.TestState;
import com.salesforce.cte.datamodel.client.TestExecution;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestSignal;
import com.salesforce.cte.datamodel.client.TestSignalEnum;
import com.salesforce.cte.datamodel.client.TestStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegistryTest {
    
    private Path root;
    
    @Before
    public void setup() throws IOException {
        root = Files.createTempDirectory("testadvisor");
    }

    @Test
    public void registryPropertiesTest() throws IOException{      
        Registry registry = new Registry(root);
        Properties regProperties = registry.getRegistryProperties();   
        regProperties.setProperty("ClientRegistryGuid", "02957862-f1d9-475a-8ce0-d7ec128ccf42");     
        regProperties.setProperty("SandboxInstance", "CS46");
        regProperties.setProperty("SandboxOrgId", "00D9A0000009HXt");
        regProperties.setProperty("SandboxOrgName", "Pleasekeep");
        regProperties.setProperty("TestSuiteName", "Schneider-Sales-Clone");

        assertEquals(root,registry.getRegistryRoot());
        assertEquals("02957862-f1d9-475a-8ce0-d7ec128ccf42", regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("CS46", regProperties.getProperty("SandboxInstance"));
        assertEquals("00D9A0000009HXt", regProperties.getProperty("SandboxOrgId"));
        assertEquals("Pleasekeep", regProperties.getProperty("SandboxOrgName"));
        assertEquals("Schneider-Sales-Clone", regProperties.getProperty("TestSuiteName"));
    }

    @Test
    public void registryPropertiesSaveTest() throws IOException{
        Registry registry = new Registry(root);
        registry.saveRegistryProperty("ClientRegistryGuid", "02957862-f1d9-475a-8ce0-d7ec128ccf42");     
        registry.saveRegistryProperty("SandboxInstance", "CS46");
        registry.saveRegistryProperty("SandboxOrgId", "00D9A0000009HXt");
        registry.saveRegistryProperty("SandboxOrgName", "Pleasekeep");
        registry.saveRegistryProperty("TestSuiteName", "Schneider-Sales-Clone");

        Properties regProperties = registry.getRegistryProperties();   
        assertEquals("02957862-f1d9-475a-8ce0-d7ec128ccf42", regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("CS46", regProperties.getProperty("SandboxInstance"));
        assertEquals("00D9A0000009HXt", regProperties.getProperty("SandboxOrgId"));
        assertEquals("Pleasekeep", regProperties.getProperty("SandboxOrgName"));
        assertEquals("Schneider-Sales-Clone", regProperties.getProperty("TestSuiteName"));
    }

    @Test
    public void createRegistryPropertiesTest() throws IOException {
        Registry registry = new Registry(root.resolve("subfolder").resolve("other"));
        Properties regProperties = registry.getRegistryProperties();    
        assertNotNull(regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("no", regProperties.getProperty("portal.token.encrypted"));
        assertTrue(regProperties.getProperty("portal.refreshtoken").isEmpty());
    }

    @Test
    public void testGetUnprocessedList() throws IOException{
        //setup
        Path testrun = createTestRun(0);
        Files.createFile(testrun.resolve(Registry.SIGNAL_FILENAME));
        Path testrun2 = createTestRun(2);

        //test
        Registry registry = new Registry(root);
        List<Path> list = registry.getUnprocessedTestRunList();
        assertEquals(1, list.size());
        assertEquals(0,list.get(0).compareTo(testrun2));
    }

    @Test
    public void testGetReadyToUploadList() throws IOException{
        //setup
        Path testrun = createTestRun(0);
        Files.createFile(testrun.resolve(Registry.SIGNAL_FILENAME));
        Path testrun2 = createTestRun(2);
        Files.createFile(testrun2.resolve(Registry.PORTAL_RECORD_FILENAME));

        //test
        Registry registry = new Registry(root);
        List<Path> list = registry.getReadyToUploadTestRunList();
        assertEquals(1, list.size());
        assertEquals(0,list.get(0).toAbsolutePath()
            .compareTo(testrun.resolve(Registry.SIGNAL_FILENAME).toAbsolutePath()));
    }

    @Test
    public void testSaveTestRunSignal() throws IOException{
        Registry registry = new Registry(root);

        Path testrun = createTestRun(0);
        String testrunId = registry.getTestRunId(testrun);

        TestRunSignal testRunSignal = createTestRunSignal(testrunId);

        String filename = registry.saveTestRunSignal(testRunSignal);

        TestRunSignal testRunSignal2 = registry.getTestRunSignal(Paths.get(filename));

        assertEquals(testRunSignal.clientRegistryGuid, testRunSignal2.clientRegistryGuid);
    }

    @Test
    public void testSaveResponse() throws IOException{
        Registry registry = new Registry(root);
        Path testrun = createTestRun(0);

        String text = "some response";
        registry.savePortalResponse(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT), text);

        List<String> responses = Files.readAllLines(testrun.resolve(Registry.PORTAL_RECORD_FILENAME));
        
        assertEquals(1, responses.size());
        assertEquals(text, responses.get(0));

    }

    @Test
    public void testGetTestResultList() throws IOException{
        //setup
        Path testrun = createTestRun(0);
        Files.createFile(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun.resolve(Registry.PORTAL_RECORD_FILENAME));

        //test
        Registry registry = new Registry(root);
        Path path = registry.getTestAdvisorTestResultFile(testrun);
        assertEquals(0,path.toAbsolutePath()
            .compareTo(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT).toAbsolutePath()));
    }

    @Test
    public void testGetTestRunId() throws IOException{
        Path testrun1 = createTestRun(0);

        //test
        Registry registry = new Registry(root);
        String testrunId = registry.getTestRunId(testrun1);

        assertEquals(testrun1.getName(testrun1.getNameCount()-1).toString(), testrunId);
    }

    @Test
    public void testGetAllTestRunList() throws IOException {
        Path testrun1 = createTestRun(0);
        Path testrun2 = createTestRun(1000);
        Path testrun3 = createTestRun(300);
        //test
        Registry registry = new Registry(root);
        List<Path> allTestRunList = registry.getAllTestRuns();

        assertEquals(3,allTestRunList.size());
        assertEquals(testrun2.toString(), allTestRunList.get(0).toString());
        assertEquals(testrun3.toString(), allTestRunList.get(1).toString());
        assertEquals(testrun1.toString(), allTestRunList.get(2).toString());
    }

    @Test
    public void testFindBeforeTestRunList() throws IOException{
        Path testrun1 = createTestRun(0);
        Path testrun2 = createTestRun(1000);
        Path testrun3 = createTestRun(300);
        //test
        Registry registry = new Registry(root);
        List<Path> beforeList = registry.findBeforeTestRunList(testrun1);
        assertEquals(0,beforeList.size());

        beforeList = registry.findBeforeTestRunList(testrun3);
        assertEquals(1,beforeList.size());
        assertEquals(testrun1.toString(), beforeList.get(0).toString());

        beforeList = registry.findBeforeTestRunList(testrun2);
        assertEquals(2,beforeList.size());
        assertEquals(testrun3.toString(), beforeList.get(0).toString());
        assertEquals(testrun1.toString(), beforeList.get(1).toString());
    }

    @Test
    public void testGetBaseline() throws IOException{
        Registry registry = new Registry(root);

        Path testrun1 = createTestRun(0);
        saveTestAdvisorResult(testrun1, createTestAdvisorResult());

        Path testrun2 = createTestRun(1000);
        saveTestAdvisorResult(testrun2, createTestAdvisorResult());

        registry.getAllTestRuns();
        Path baseline = registry.getBaselineTestRun(testrun2, "testcasePass");
        assertEquals(testrun1, baseline);
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

    private Path createTestRun(int plusSeconds){
        DateTimeFormatter taDateFormatter = DateTimeFormatter.ofPattern(Registry.TESTADVISOR_TESTRUN_PATTERN_STRING);
        String testRunId = Registry.TESTADVISOR_TESTRUN_PREFIX + taDateFormatter.format(
            OffsetDateTime.now( ZoneOffset.UTC ).plusSeconds(plusSeconds));
        Path testrun = root.resolve(testRunId);
        testrun.toFile().mkdirs();

        return testrun;
    }

    private TestRunSignal createTestRunSignal(String testrunId){
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
        testExecution.testSignals.add(createTestSignal("Selenium","Exception"));
        testExecution.testSignals.add(createTestSignal("Automation","Exception"));
        testRunSignal.testExecutions.add(testExecution);

        testExecution = createTestExecution("testcasePass",TestStatus.PASS);
        testExecution.testSignals.add(createTestSignal("Selenium","Exception"));
        testExecution.testSignals.add(createTestSignal("Automation","Exception"));
        testRunSignal.testExecutions.add(testExecution);

        return testRunSignal;
    }

    private TestExecution createTestExecution(String name, TestStatus status){
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

    private TestSignal createTestSignal(String signalName, String signalValue){
        Instant now = Instant.now();

        TestSignal signal = new TestSignal();
        signal = new TestSignal();
        signal.signalName = signalName;
        signal.signalValue = signalValue;
        signal.signalTime = now.plusSeconds(2);
        signal.errorMessage = "PreDefined";
        signal.baselinScreenshotRecorderNumber = 1;
        signal.screenshotRecorderNumber = 1;
        signal.previousSignalTime = signal.signalTime.minusSeconds(5);
        signal.locatorHash = "locator";
        signal.screenshotDiffRatio = 5;
        signal.seleniumCmd = "click";
        signal.screenshotDiffAreas = new ArrayList<>();
        signal.screenshotDiffAreas.add(new Rectangle(0, 0, 100, 100));

        return signal;
    }

    private TestAdvisorResult createTestAdvisorResult(){
        TestAdvisorResult testAdvisorResult = new TestAdvisorResult();
        testAdvisorResult.version = "1.0.0";
        testAdvisorResult.buildStartTime = Instant.now();
        testAdvisorResult.buildEndTime = testAdvisorResult.buildStartTime.plusSeconds(5);
        testAdvisorResult.testCaseExecutionList = new ArrayList<>();

        TestCaseExecution testCaseExecution = new TestCaseExecution();
        testCaseExecution.browser = "chrome";
        testCaseExecution.browserVersion = "89";
        testCaseExecution.testName = "testcasePass";
        testCaseExecution.startTime = Instant.now();
        testCaseExecution.endTime = testCaseExecution.startTime.plusSeconds(5);
        testCaseExecution.testStatus = com.salesforce.cte.common.TestStatus.PASSED;
        testCaseExecution.screenResolution = "1920*1080";

        testAdvisorResult.testCaseExecutionList.add(testCaseExecution);

        return testAdvisorResult;

    }

    private void saveTestAdvisorResult(Path testrun, TestAdvisorResult result) throws JsonGenerationException, JsonMappingException, IOException{
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
							.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		ObjectWriter objectWriter = objectMapper.writer(new DefaultPrettyPrinter());

        Path outputFilePath = testrun.resolve("test-result.json");
	    objectWriter.withDefaultPrettyPrinter().writeValue(outputFilePath.toFile(), result);
    }

}
