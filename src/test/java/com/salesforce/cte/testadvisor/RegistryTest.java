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

import com.salesforce.cte.datamodel.client.TestRunSignal;

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

        TestRunSignal testRunSignal = new TestRunSignal();
        Instant now = Instant.now();
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
        testRunSignal.testRunId = testrunId;

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

}
