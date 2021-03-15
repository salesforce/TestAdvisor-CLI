package com.salesforce.bst.extension;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yibing Tao
 */
public class TestRunSignalTest {

  @Test
  public void testSerialization() throws JsonGenerationException, JsonMappingException, IOException {
    Customer customer = new Customer("123","cs997","00D9A0000009IsD","bst");
    Registry registry = new Registry(UUID.randomUUID(),"bst registry", "0.0.1");
    TestSignal signal = new TestSignal("TestCase1", System.currentTimeMillis(), 
                    System.currentTimeMillis(), TestResult.PASS);
    signal.setTestFailures(new ArrayList<>());
    signal.getTestFailures().add(new TestFailure("AutomationLog","AssertTrue", System.currentTimeMillis()));
    signal.getTestFailures().add(new TestFailure("SeleniumLog","ElementNotFound", System.currentTimeMillis()));
   
    TestRunSignal testRunSignal = new TestRunSignal(customer,registry, 
                                System.currentTimeMillis(), System.currentTimeMillis());
    testRunSignal.getTestSignals().add(signal);

    ObjectMapper objectMapper = new ObjectMapper();
    String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testRunSignal);
    
    assertTrue(content.contains("testSignals"));

    BufferedWriter writer = new BufferedWriter(new FileWriter("target/sample.json"));
    writer.write(content);
    writer.close();
  }
}
