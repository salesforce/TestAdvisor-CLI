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
    Customer customer = new Customer("123","cs997","1234567890","bst");
    Registry registry = new Registry(UUID.randomUUID());
    TestFailure testFailure = new TestFailure("AssertTrue", System.currentTimeMillis());
    TestFailure seleniumFailure = new TestFailure("ElementNotFound", System.currentTimeMillis());
    TestSignal signal = new TestSignal("TestCase1", System.currentTimeMillis(), 
                    System.currentTimeMillis(), TestResult.PASS);
    signal.setTestFailures(new ArrayList<>());
    signal.setSeleniumFailures(new ArrayList<>());
    signal.getTestFailures().add(testFailure);
    signal.getSeleniumFailures().add(seleniumFailure);
    TestRunSignal testRunSignal = new TestRunSignal(customer,registry,UUID.randomUUID(), 
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
