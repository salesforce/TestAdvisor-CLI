package com.salesforce.cqe.processor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import com.salesforce.cqe.datamodel.client.TestRunSignal;

import org.junit.Test;

public class ProcessorTest{

    @Test
    public void testProcessTestNGXml() throws JAXBException, ParseException, IOException{
        TestRunSignal testRunSignal = new TestRunSignal();
        testRunSignal.clientBuildId = "123";
        testRunSignal.clientCliVersion = "1.0.1";
        testRunSignal.clientLibraryVersion = "1.0.1";
        testRunSignal.clientRegistryGuid = UUID.randomUUID();
        testRunSignal.sandboxInstance = "CS997";
        testRunSignal.sandboxOrgId = "00D9A0000009IsD";
        testRunSignal.sandboxOrgName = "bst";
        testRunSignal.testSuiteName = "testSuite1";

        try(InputStream is = getClass().getClassLoader().getResourceAsStream("xml/testng-results.xml")){
            testRunSignal = Processor.processTestNGSignal(is, testRunSignal);
        }
        
        assertEquals(32, testRunSignal.testExecutions.size());
    }
}