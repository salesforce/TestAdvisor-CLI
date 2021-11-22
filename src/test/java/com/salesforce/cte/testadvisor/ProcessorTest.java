package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.salesforce.cte.adapter.TestAdvisorAdapter;
import com.salesforce.cte.adapter.TestAdvisorResultAdapter;
import com.salesforce.cte.adapter.TestNGAdapter;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestStatus;
import com.salesforce.cte.helper.ProcessException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessorTest{

    private Path root;
    private Registry registry;
    private Processor processor;

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
    public void testProcessTestAdvisorResult() throws IOException, ProcessException{
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
    private void testCompareTestCaseExecution(){

        Path run1 = RegistryHelper.createTestRun(registry, 0);
        Path run2 = RegistryHelper.createTestRun(registry, 1000);

       


        //processor.compareTestCaseExecution(baseline, current, signalList)
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