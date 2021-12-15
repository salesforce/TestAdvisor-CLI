/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.github.romankh3.image.comparison.model.Rectangle;
import com.salesforce.cte.common.TestCaseExecution;
import com.salesforce.cte.common.TestAdvisorResult;
import com.salesforce.cte.datamodel.client.RectangleDeserializer;
import com.salesforce.cte.datamodel.client.RectangleSerializer;
import com.salesforce.cte.datamodel.client.TestRunSignal;

/**
 * @author Yibing Tao
 * Registry class manages TestAdvisor registry, including TestAdvisor properties
 * list test runs and save test run signals.
 */
public class Registry {
    
    private static final Logger LOGGER = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );

    public static final String SIGNAL_FILENAME = "test-signal.json";
    public static final String PORTAL_RECORD_FILENAME = "test-result.record";
    public static final String TESTADVISOR_TESTRUN_PREFIX = "TestRun-";
    public static final String TESTADVISOR_TESTRUN_PATTERN_STRING = "yyyyMMdd-HHmmss";
    public static final String TESTADVISOR_PROPERTIES_FILENAME = "testadvisor.properties";
    public static final String TESTADVISOR_DEFAULT_REGISGRY = ".testadvisor"; //TODO: what about different platform
    public static final String TESTADVISOR_TEST_RESULT = "test-result.json";
    public static final String TESTADVISOR_PROPERTY_CLIENT_GUID = "ClientRegistryGuid";
    
    private List<Path> allTestRunList = new ArrayList<>();
    private Properties registryConfig = new Properties();
    private Path registryRoot;
    public Path getRegistryRoot(){
        return registryRoot;
    }

    public Registry() throws IOException{
        //get registry root
        if (System.getenv("TESTADVISOR") == null){
            //env not set
            registryRoot = Paths.get(System.getProperty("user.dir"))
                            .resolve(Paths.get(TESTADVISOR_DEFAULT_REGISGRY))
                            .toAbsolutePath();
        }
        else
            registryRoot = Paths.get(System.getenv("TESTADVISOR"));
        initRegistry();
    }

    public Registry(Path root) throws IOException{
        registryRoot = root;
        initRegistry();
    }

    /**
     * Init Registry
     * @throws IOException
     * This exception will be thrown when it fails to access registry configruation file
     */
    private void initRegistry() throws IOException {
        //create registry root folder first
        if (!registryRoot.toFile().exists())
            registryRoot.toFile().mkdirs();

        //create property file if necessary
        if (!registryRoot.resolve(TESTADVISOR_PROPERTIES_FILENAME).toFile().exists())
            createRegistryProperties();

        getAllTestRuns();
    }

    /**
     * Get TestAdvisor registry configuration from its default properties file
     * @return
     * TestAdvisor registry configruation properties
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public Properties getRegistryProperties() throws IOException{
        try(InputStream input = Files.newInputStream(registryRoot.resolve(TESTADVISOR_PROPERTIES_FILENAME))){
            registryConfig.load(input);
        }
        return registryConfig;
    }

    /**
     * set and save registry properties
     * @param key
     * Property key
     * @param value
     * Property value
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public void saveRegistryProperty(String key, String value) throws IOException{
        registryConfig.setProperty(key, value);
        saveRegistryProperties();
    }

    /**
     * Save the TestAdvisor registry configruation properties
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public void saveRegistryProperties() throws IOException{
        //Generate a random UUID if not present yet
        String guid = registryConfig.getProperty(TESTADVISOR_PROPERTY_CLIENT_GUID,"");
        if ( guid.isEmpty())
            registryConfig.setProperty(TESTADVISOR_PROPERTY_CLIENT_GUID, UUID.randomUUID().toString());

        //Save all properites
        try(OutputStream output = Files.newOutputStream(registryRoot.resolve(TESTADVISOR_PROPERTIES_FILENAME))){
			registryConfig.store(output, null);
		}
    }

    /**
     * Save the test run signal to registry
     * @param testRunSignal
     * Test run signal object, including test run id
     * @return
     * test signal file name in registry
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public String saveTestRunSignal(TestRunSignal testRunSignal) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Rectangle.class, new RectangleSerializer());
        objectMapper.registerModule(module);
        String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testRunSignal);
        //create test run folder if necessary
        registryRoot.resolve(testRunSignal.testRunId).toFile().mkdirs();
        String fileName = registryRoot.resolve(testRunSignal.testRunId).resolve(SIGNAL_FILENAME).toString();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))){
          writer.write(content);
        }
        return fileName;
    }

    /**
     * Get a list of Path with start with TestRun and doesn't contain a test-signal file
     * @return
     * List of Path object represent test runs haven't process yet
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public List<Path> getUnprocessedTestRunList() throws IOException{
        getAllTestRuns();
        List<Path> unProcessedTestRunList = new ArrayList<>();
        for(Path testRun : allTestRunList){
            try(Stream<Path> pathStream = Files.walk(testRun, 1)){
                if (pathStream.noneMatch(name -> name.endsWith(SIGNAL_FILENAME)))
                    unProcessedTestRunList.add(testRun);
            }
        }
        return unProcessedTestRunList;
    }

    /**
     * Get a list of test run as Path in TestAdvisor registry which is ready to upload
     * @return
     * list of test run which is ready for upload
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public List<Path> getReadyToUploadTestRunList() throws IOException{
        getAllTestRuns();
        // filter test run with signal file
        List<Path> readyList = new ArrayList<>();
        for(Path testRun : allTestRunList){
            try(Stream<Path> pathStream = Files.walk(testRun, 1)){
                if (pathStream.anyMatch(name -> name.endsWith(SIGNAL_FILENAME)))
                    readyList.add(testRun);
            }
        }
        // filter test run without record file (not upload yet)
        List<Path> uploadList = new ArrayList<>();
        for(Path testRun : readyList){
            try(Stream<Path> pathStream = Files.walk(testRun, 1)){
                if (pathStream.noneMatch(name -> name.endsWith(PORTAL_RECORD_FILENAME)))
                    uploadList.add(testRun.resolve(SIGNAL_FILENAME));
            }
        }

        return uploadList;
    }

    /**
     * Get all list of test runs from registry, the output list will be 
     * sorted by test run time stamp. Lastest test run on top.
     * @return Sorted list of all test runs in registry, latest test run first
     * @throws IOException throw this exception when fail to find test runs
     */
    public List<Path> getAllTestRuns() throws IOException{
        // get all test run from registry
        allTestRunList.clear();
        try(Stream<Path> pathStream = Files.walk(registryRoot,1)){
            allTestRunList =  pathStream.filter(Files::isDirectory)
                                        .filter(path -> path.toString().contains(TESTADVISOR_TESTRUN_PREFIX))
                                        .filter(path -> Files.exists(path.resolve(TESTADVISOR_TEST_RESULT)))
                                        .collect(Collectors.toList());
        }

        allTestRunList.sort(this::compareTestRun);
        Collections.reverse(allTestRunList);
        
        return allTestRunList;
    }

    /**
     * Get baseline test run from all test run list for current test execution in current test run
     * The baseline run will be test run contains last known good (LKG) test execution.
     * If no LKG was found, the last test run will be pick
     * @param currentTestRun current test run
     * @param testCaseName current test case name
     * @return Baseline test run path. null if no baseline was found.
     * @throws IOException throws this exception when fails to find baseline test run 
     */
    public Path getBaselineTestRun(Path currentTestRun, String testCaseName) throws IOException{
        List<Path> beforeTestRunList = findBeforeTestRunList(currentTestRun);
        
        // no test run found
        if (beforeTestRunList.isEmpty())
            return null;

        //all test run before current run should order by create date and latest on top
        for(Path testrun : beforeTestRunList){
            if(containsPassedTest(testrun, testCaseName))
                return testrun;
        }

        //no LKG run found, return latest run.
        return beforeTestRunList.get(0);
    }

    /**
     * Check whether current test run contains test result for passed test case
     * @param testRun
     * current test run path
     * @param test
     * current test execution
     * @return
     * true -- if current test run contains test result for passed current test case
     * false -- otherwise
     * @throws IOException throws this exception when fails to access test run 
     */
    private boolean containsPassedTest(Path testRun, String testCaseName) throws IOException{
        TestAdvisorResult result = getTestAdvisorResult(testRun);
        for(TestCaseExecution test : result.testCaseExecutionList){
            if (test.testName.equals(testCaseName) && test.testStatus.equals(com.salesforce.cte.common.TestStatus.PASSED))
                return true;
        }
        return false;
    }

    /**
     * Get list of test runs from all test run list which is before current test run 
     * based on test run created time
     * @param currentTestRun current test run
     * @return
     * List of test run path which is created before current test run
     * returned test run list order by created time, latest first
     */
    public List<Path> findBeforeTestRunList(Path currentTestRun) {
        List<Path> beforeList = new ArrayList<>();

        for(Path testrun : allTestRunList){
            if(compareTestRun(testrun, currentTestRun)<0){
                beforeList.add(testrun);
            }
        }

        return beforeList;
    }

    /**
     * Compare 2 test run based on create time
     * @param testrun1 testrun1
     * @param testrun2 testrun2
     * @return
     * 1 - test run 1 created after test run 2
     * -1 - test run 1 created before test run 2
     * 0 - test run 1 and 2 created at same time
     */
    private int compareTestRun(Path testrun1, Path testrun2){
        ZonedDateTime testRun1Time = getTestRunCreatedTime(testrun1);
        ZonedDateTime testRun2Time = getTestRunCreatedTime(testrun2);

        return testRun1Time.compareTo(testRun2Time);
    }
    /**
     * Get test run signal object for current test run from registry
     * @param path
     * Path to current test run
     * @return
     * Test run singal object
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     * @throws JsonMappingException
     * This exception is thrown when it failed to parse test result json 
     * @throws JsonParseException
     * This exception is thrown when it failed to parse test result json
     */
    public TestRunSignal getTestRunSignal(Path path) throws IOException{
        if (path == null) return null;
        if (!path.getParent().resolve(SIGNAL_FILENAME).toFile().exists()
            || !path.getParent().resolve(SIGNAL_FILENAME).toFile().canRead())
            return null;
        String fileName = path.getParent().resolve(SIGNAL_FILENAME).toAbsolutePath().toString();
        
        try(InputStream is = new FileInputStream(fileName)){
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Rectangle.class, new RectangleDeserializer());
            objectMapper.registerModule(module);
            return objectMapper.readValue(is, TestRunSignal.class);
        }
    }

    /**
     * Get test advisor result for the test run
     * @param testrun current test run
     * @return TestAdvisorResult
     * @throws IOException throws this exception when failed to access test advisor result file
     */
    public TestAdvisorResult getTestAdvisorResult(Path testrun) throws IOException{
        Path testResultFilePath = testrun.resolve(TESTADVISOR_TEST_RESULT).toAbsolutePath();
        
        if (!Files.exists(testResultFilePath) || !Files.isReadable(testResultFilePath)){
            return new TestAdvisorResult();
        }

        try(InputStream is = new FileInputStream(testResultFilePath.toString())){
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return objectMapper.readValue(is, TestAdvisorResult.class);
        }
    }

    /**
     * Save current test run response to registry
     * @param path
     * Path to current test run
     * @param response
     * Portal response for signal upload
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public void savePortalResponse(Path path, String response) throws IOException{
        String filename = path.getParent().resolve(PORTAL_RECORD_FILENAME).toAbsolutePath().toString();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(filename))){
            writer.write(response);
        }
    }

    /**
     * Extract the test run id from path based on TestAdvisor registry specification
     * @param path
     * Path object point to a test run
     * @return
     * Test run id if Path object contains a test run id, otherwise return an empty 
     * string
     */
    public String getTestRunId(Path path){
        if (path == null) return "";
        return getTestRunId(path.toAbsolutePath().toString());
    }

    /**
     * Extract the test run id from path based on TestAdvisor registry specification
     * @param path
     * String of path to a test run
     * @return
     * Test run id if Path object contains a test run id, otherwise return test run id
     * based on current local time
     */
    public String getTestRunId(String path){
        if (path == null || path.isEmpty()) return "";
        DateTimeFormatter taDateFormatter = DateTimeFormatter.ofPattern(TESTADVISOR_TESTRUN_PATTERN_STRING);
        String testRunId = TESTADVISOR_TESTRUN_PREFIX + taDateFormatter.format(OffsetDateTime.now( ZoneOffset.UTC ));
        Pattern pattern = Pattern.compile("(TestRun-\\d{8}-\\d{6})");
        Matcher matcher = pattern.matcher(path);
        return matcher.find( ) ? matcher.group(0) : testRunId;
    }

    /**
     * Get test run path from test run id
     * @param testRunId test run id
     * @return test run path, or null if can't find the match path
     */
    public Path getTestRunPath(String testRunId){
        for(Path path : allTestRunList){
            if (path.endsWith(testRunId))
                return path;
        }
        return null;
    }

    /**
     * Get test run created time stamp
     * @param path
     * Path to the test run
     * @return
     * Test run created time
     * or EPOCH (1970/1/1) if path is null or invalid
     */
    private ZonedDateTime getTestRunCreatedTime(Path path){
        if (path == null) return Instant.EPOCH.atZone(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TESTADVISOR_TESTRUN_PATTERN_STRING);    
        Pattern pattern = Pattern.compile("(\\d{8}-\\d{6})");
        Matcher matcher = pattern.matcher(path.toAbsolutePath().toString());
        if  (matcher.find())
            return LocalDateTime.parse(matcher.group(0),formatter).atZone(ZoneId.of("UTC"));
        
        LOGGER.log(Level.WARNING, "Path object doesn't contain created time, path:{0}",path.toAbsolutePath());
        return Instant.EPOCH.atZone(ZoneId.of("UTC"));
    }

    /**
     * Get the TestAdvisor test result file in registry
     * @param testRun
     * Path to test run
     * @return
     * Path object of TestAdvisor test result file
     * or null if no test result file found
     * @throws IOException
     * Throws IOException when failed to access test run folder or files
     */
    public Path getTestAdvisorTestResultFile(Path testRun) throws IOException{
        try(Stream<Path> pathStream = Files.walk(testRun,1)){
            return pathStream.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(TESTADVISOR_TEST_RESULT))
                            .findFirst()
                            .orElse(null);                                     
        }
    }

    /**
     * create a new set of empty registry properties and save it
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    private void createRegistryProperties() throws IOException{
        registryConfig.clear();
        registryConfig.put("SandboxInstance", "");
        registryConfig.put("SandboxOrgName", "");
        registryConfig.put("SandboxOrgId", "");
        registryConfig.put("TestSuiteName", "");
        registryConfig.put("auth.url", "https://test.salesforce.com");
        registryConfig.put("portal.clientid", "clientid");
        registryConfig.put("portal.url", "");
        registryConfig.put("portal.token.encrypted", "no");
        registryConfig.put("portal.accesstoken", "");
        registryConfig.put("portal.refreshtoken", "");

        saveRegistryProperties();
    }

    
}
