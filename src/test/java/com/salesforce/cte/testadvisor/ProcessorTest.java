package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.salesforce.cte.adapter.TestAdvisorAdapter;
import com.salesforce.cte.adapter.TestAdvisorResultAdapter;
import com.salesforce.cte.adapter.TestNGAdapter;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.datamodel.client.TestStatus;
import com.salesforce.cte.helper.ProcessException;

import org.junit.Test;

public class ProcessorTest{

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
            Processor.process(is, testRunSignal,adapter);
        }
        
        assertEquals(32, testRunSignal.testExecutions.size());
    }

    @Test
    public void testProcessTestAdvisorResult() throws IOException, ProcessException{
        TestRunSignal testRunSignal = new TestRunSignal();
        testRunSignal.clientBuildId = "123";
        testRunSignal.clientCliVersion = "1.0.1";
        testRunSignal.clientLibraryVersion = "1.0.1";
        testRunSignal.clientRegistryGuid = UUID.randomUUID();
        testRunSignal.sandboxInstance = "CS997";
        testRunSignal.sandboxOrgId = "00D9A0000009IsD";
        testRunSignal.sandboxOrgName = "bst";
        testRunSignal.testSuiteName = "testSuite1";
        TestAdvisorAdapter adapter = new TestAdvisorResultAdapter();
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("json/test-result.json")){
            Processor.process(is, testRunSignal,adapter);
        }
        
        assertEquals(12, testRunSignal.testExecutions.size());
        assertEquals(TestStatus.FAIL, testRunSignal.testExecutions.get(0).status);
        assertEquals(TestStatus.PASS, testRunSignal.testExecutions.get(1).status);
    }

}