package com.salesforce.bst.extension;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Test;

import junit.framework.Assert;

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
    
    TestRunSignal testRunSignal = new TestRunSignal(customer,registry,UUID.randomUUID(), 
                                System.currentTimeMillis(), System.currentTimeMillis());
    
    ObjectMapper objectMapper = new ObjectMapper();
    String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testRunSignal);
    
    assertTrue(content.contains("testSignals"));

  }
}
