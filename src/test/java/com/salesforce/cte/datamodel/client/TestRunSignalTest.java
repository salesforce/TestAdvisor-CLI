package com.salesforce.cte.datamodel.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.romankh3.image.comparison.model.Rectangle;

/**
 * @author Yibing Tao
 */
public class TestRunSignalTest {

  ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  @Test
  public void testSerialization() throws JsonGenerationException, JsonMappingException, IOException {
    
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

    TestExecution testExecution = new TestExecution();
    testExecution.startTime = now;
    testExecution.endTime = now.plusSeconds(5);
    testExecution.status = TestStatus.FAIL;
    testExecution.testCaseName = "testcase1";
    testExecution.testSignals = new ArrayList<>();
    testExecution.similarity = 50;

    TestSignal signal = new TestSignal();
    signal.signalName = "AUTOMATION";
    signal.signalValue = "org.testng.Assert.assertEquals";
    signal.signalTime = now.plusSeconds(1);
    testExecution.testSignals.add(signal);
    signal = new TestSignal();
    signal.signalName = "SELENIUM";
    signal.signalValue = "org.openqa.selenium.NoSuchElementException";
    signal.signalTime = now.plusSeconds(2);
    signal.errorMessage = "PreDefined";
    signal.baselinScreenshotRecorderNumber = 1;
    signal.screenshotRecorderNumber = 1;
    signal.previousSignalTime = signal.signalTime.minusSeconds(5);
    signal.locatorHash = "locator";
    signal.screenshotDiffRatio = 5;
    signal.seleniumCmd = "click";
    signal.screenshotDiffAreas = new ArrayList<>();
    signal.screenshotDiffAreas.add(new Rectangle(0, 0, 100, 100));
    testExecution.testSignals.add(signal);
    testRunSignal.testExecutions.add(testExecution);

    SimpleModule module = new SimpleModule();
    module.addSerializer(Rectangle.class, new RectangleSerializer());
    module.addDeserializer(Rectangle.class, new RectangleDeserializer());
    objectMapper.registerModule(module);
    String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testRunSignal);

    // try(BufferedWriter writer = new BufferedWriter(new FileWriter("target/sample.json"))){
    //   writer.write(content);
    // }

    TestRunSignal testRunSignal2 = objectMapper.readValue(content, TestRunSignal.class);
    assertEquals(testRunSignal.clientBuildId, testRunSignal2.clientBuildId);
    assertEquals(testRunSignal.sandboxInstance, testRunSignal2.sandboxInstance);
    assertEquals(testRunSignal.sandboxOrgId, testRunSignal2.sandboxOrgId);
    assertEquals(testRunSignal.sandboxOrgName, testRunSignal2.sandboxOrgName);
    assertEquals(testRunSignal.testRunId, testRunSignal2.testRunId);
  }

  @Test
  public void testRegistryDeserialization() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignal = objectMapper.readValue(json, TestRunSignal.class);
    assertEquals(UUID.fromString("202314a1-f4b4-49ee-a65c-0ec08f4b7ed1"), testRunSignal.clientRegistryGuid);
    assertEquals("testSuite1", testRunSignal.testSuiteName);
    assertEquals("1.0.1", testRunSignal.clientLibraryVersion);
  }  

  @Test
  public void testTestRunPropertiesDeserialization()throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignal = objectMapper.readValue(json, TestRunSignal.class);
    assertEquals("2021-05-25T03:36:13.420563Z", testRunSignal.buildStartTime.toString());
    assertEquals("2021-05-25T03:36:18.420563Z", testRunSignal.buildEndTime.toString());
  }

  @Test
  public void testTestSignalDeserialization() throws IOException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("json/sample.json");
    String json = convertInputStreamToString(is);
    
    TestRunSignal testRunSignal = objectMapper.readValue(json, TestRunSignal.class);
    assertEquals(1, testRunSignal.testExecutions.size());
    assertEquals("testcase1", testRunSignal.testExecutions.get(0).testCaseName);
    assertEquals("2021-05-25T03:36:13.420563Z", testRunSignal.testExecutions.get(0).startTime.toString());
    assertEquals("2021-05-25T03:36:18.420563Z",  testRunSignal.testExecutions.get(0).endTime.toString());
    assertEquals(TestStatus.FAIL, testRunSignal.testExecutions.get(0).status);
    assertEquals(2, testRunSignal.testExecutions.get(0).testSignals.size());
    assertEquals("AUTOMATION", testRunSignal.testExecutions.get(0).testSignals.get(0).signalName);
    assertEquals("org.testng.Assert.assertEquals", testRunSignal.testExecutions.get(0).testSignals.get(0).signalValue);
    assertEquals("2021-05-25T03:36:14.420563Z", testRunSignal.testExecutions.get(0).testSignals.get(0).signalTime.toString());
    assertEquals("SELENIUM", testRunSignal.testExecutions.get(0).testSignals.get(1).signalName);
    assertEquals("org.openqa.selenium.NoSuchElementException", testRunSignal.testExecutions.get(0).testSignals.get(1).signalValue);
    assertEquals("2021-05-25T03:36:15.420563Z", testRunSignal.testExecutions.get(0).testSignals.get(1).signalTime.toString());
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
