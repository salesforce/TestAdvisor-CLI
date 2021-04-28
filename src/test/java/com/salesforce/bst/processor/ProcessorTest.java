package com.salesforce.bst.processor;

import static org.junit.Assert.assertEquals;

import java.io.FileReader;
import java.io.InputStream;
import java.text.ParseException;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import com.salesforce.bst.datamodel.client.Customer;
import com.salesforce.bst.datamodel.client.Registry;
import com.salesforce.bst.datamodel.client.TestRunSignal;

import org.junit.Test;

public class ProcessorTest{

    @Test
    public void testTestNGXml() throws JAXBException, ParseException{
        Customer customer = new Customer("123","cs997","00D9A0000009IsD","bst");
        Registry registry = new Registry(UUID.randomUUID(),"bst registry", "0.0.1");
        InputStream is = getClass().getClassLoader().getResourceAsStream("xml/testng-results.xml");
        TestRunSignal testRunSignals = Processor.processTestNGSignal(is, customer, registry);
        assertEquals("123", testRunSignals.getCustomer().getCustomerId());
        assertEquals(243, testRunSignals.getTestSignals().size());
    }
}