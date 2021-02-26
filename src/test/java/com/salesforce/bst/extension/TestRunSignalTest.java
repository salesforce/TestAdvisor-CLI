package com.salesforce.bst.extension;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
    TestSignal signal = new TestSignal("TestCase1", System.currentTimeMillis(), 
                    System.currentTimeMillis(), TestResult.PASS);
    TestRunSignal testRunSignal = new TestRunSignal(customer,registry,UUID.randomUUID(), 
                                System.currentTimeMillis(), System.currentTimeMillis());
    testRunSignal.getTestSignals().add(signal);

    ObjectMapper objectMapper = new ObjectMapper();
    String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testRunSignal);
    
    assertTrue(content.contains("testSignals"));
  }
}
