package com.salesforce.bst.datamodel.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
    List<TestSignal> signals = new ArrayList<TestSignal>();
    signals.add(signal);
    
    TestRunSignal testRunSignal = new TestRunSignal(customer,registry, 
                                System.currentTimeMillis(), System.currentTimeMillis(), 
                                signals);
    

    ObjectMapper objectMapper = new ObjectMapper();
    String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testRunSignal);
    
    assertTrue(content.contains("testSignals"));

    BufferedWriter writer = new BufferedWriter(new FileWriter("target/sample.json"));
    writer.write(content);
    writer.close();
  }

  @Test
  public void testCustomerDeserialization() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignals = new ObjectMapper().readValue(json, TestRunSignal.class);
    assertEquals("123", testRunSignals.getCustomer().getCustomerId());
    assertEquals("cs997", testRunSignals.getCustomer().getSandboxInstanceName());
    assertEquals("00D9A0000009IsD", testRunSignals.getCustomer().getSandboxOrgId());
    assertEquals("bst", testRunSignals.getCustomer().getSandboxOrgName());
  }

  @Test
  public void testRegistryDeserialization() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignals = new ObjectMapper().readValue(json, TestRunSignal.class);
    assertEquals(UUID.fromString("f185df4e-ee4f-4a99-9a57-e61f6682f49b"), testRunSignals.getRegistry().getRegistryGuid());
    assertEquals("bst registry", testRunSignals.getRegistry().getRegistryName());
    assertEquals("0.0.1", testRunSignals.getRegistry().getBstClientVersion());
  }  

  @Test
  public void testTestRunPropertiesDeserialization()throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignals = new ObjectMapper().readValue(json, TestRunSignal.class);
    assertEquals(1615772301416L, testRunSignals.getStartTime());
    assertEquals(1615772301416L, testRunSignals.getEndTime());
  }

  @Test
  public void testTestSignalDeserialization() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignals = new ObjectMapper().readValue(json, TestRunSignal.class);
    assertEquals(1, testRunSignals.getTestSignals().size());
    assertEquals("TestCase1", testRunSignals.getTestSignals().get(0).getTestName());
    assertEquals(1615772301414L, testRunSignals.getTestSignals().get(0).getStartTime());
    assertEquals(1615772301414L, testRunSignals.getTestSignals().get(0).getEndTime());
    assertEquals(TestResult.PASS, testRunSignals.getTestSignals().get(0).getTestResult());
    assertEquals(2, testRunSignals.getTestSignals().get(0).getTestFailures().size());
    assertEquals("AutomationLog", testRunSignals.getTestSignals().get(0).getTestFailures().get(0).getFailureSource());
    assertEquals("AssertTrue", testRunSignals.getTestSignals().get(0).getTestFailures().get(0).getFailureType());
    assertEquals(1615772301415L, testRunSignals.getTestSignals().get(0).getTestFailures().get(0).getFailureTime());
    assertEquals("SeleniumLog", testRunSignals.getTestSignals().get(0).getTestFailures().get(1).getFailureSource());
    assertEquals("ElementNotFound", testRunSignals.getTestSignals().get(0).getTestFailures().get(1).getFailureType());
    assertEquals(1615772301415L, testRunSignals.getTestSignals().get(0).getTestFailures().get(1).getFailureTime());
  }

  private String convertInputStreamToString(InputStream inputStream)throws IOException {
      String newLine = System.getProperty("line.separator");
      StringBuilder result = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
          String line;
          while ((line = reader.readLine()) != null) {
              result.append(line).append(newLine);
          }
      }
      return result.toString();
  }
}
