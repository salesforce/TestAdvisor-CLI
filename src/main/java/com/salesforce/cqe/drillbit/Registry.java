package com.salesforce.cqe.drillbit;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.cqe.datamodel.client.TestRunSignal;

/**
 * @author Yibing Tao
 * Registry class manages Drillbit registry, including drillbit properties
 * list test runs and save test run signals.
 */
public class Registry {
    
    private static final String SIGNAL_FILENAME = "test-signal.json";
    private static final String PORTAL_RECORD_FILENAME = "test-result.record";
    private static final String DRILLBIT_TESTRUN_PREFIX = "TestRun-";
    private static final String DRILLBIT_TESTRUN_PATTERN_STRING = "yyyyMMdd-HHmmss";
    private static final String DRILLBIT_PROPERTIES_FILENAME = "drillbit.properties";
    private static final String DRILLBIT_DEFAULT_REGISGRY = ".drillbit"; //TODO: what about different platform
     
    private Properties registryConfig = new Properties();
    private Path registryRoot;
    public Path getRegistryRoot(){
        return registryRoot;
    }

    public Registry(){
        //get registry root
        if (System.getenv("DRILLBIT_REGISTRY") == null){
            //env not set
            registryRoot = Paths.get(System.getProperty("user.dir"))
                            .resolve(Paths.get(DRILLBIT_DEFAULT_REGISGRY))
                            .toAbsolutePath();
        }
        else
            registryRoot = Paths.get(System.getenv("DRILLBIT_REGISTRY"));
    }

    public Registry(Path root){
        registryRoot = root;
    }

    /**
     * Get drillbit registry configuration from its default properties file
     * @return
     * drillbit registry configruation properties
     * @throws IOException
     */
    public Properties getRegistryProperties() throws IOException{
        try(InputStream input = Files.newInputStream(registryRoot.resolve(DRILLBIT_PROPERTIES_FILENAME))){
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
     */
    public void saveRegistryProperty(String key, String value) throws IOException{
        registryConfig.setProperty(key, value);
        saveRegistryProperties();
    }

    /**
     * Save the drillbit registry configruation properties
     * @throws IOException
     */
    public void saveRegistryProperties() throws IOException{
        try(OutputStream output = Files.newOutputStream(registryRoot.resolve(DRILLBIT_PROPERTIES_FILENAME))){
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
     */
    public String saveTestRunSignal(TestRunSignal testRunSignal) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
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
     */
    public List<Path> getUnprocessedTestRunList() throws IOException{
        
        List<Path> allTestRunList;
        try(Stream<Path> pathStream = Files.walk(registryRoot,1)){
            allTestRunList =  pathStream.filter(Files::isDirectory)
                                        .filter(path -> path.toString().contains(DRILLBIT_TESTRUN_PREFIX))
                                        .collect(Collectors.toList());
        }
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
     * Get a list of test run as Path in drillbit registry which is ready to upload
     * @return
     * @throws IOException
     */
    public List<Path> getReadyToUploadTestRunList() throws IOException{
        List<Path> allTestRunList;
        try(Stream<Path> pathStream = Files.walk(registryRoot,1)){
            allTestRunList =  pathStream.filter(Files::isDirectory)
                                        .filter(path -> path.toString().contains(DRILLBIT_TESTRUN_PREFIX))
                                        .collect(Collectors.toList());
        }
        List<Path> readyList = new ArrayList<>();
        for(Path testRun : allTestRunList){
            try(Stream<Path> pathStream = Files.walk(testRun, 1)){
                if (pathStream.anyMatch(name -> name.endsWith(SIGNAL_FILENAME)))
                    readyList.add(testRun.resolve(SIGNAL_FILENAME));
            }
        }
        return readyList;
    }

    /**
     * Get test run signal object for current test run from registry
     * @param path
     * Path to current test run
     * @return
     * Test run singal object
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public TestRunSignal getTestRunSignal(Path path) throws IOException{
        String fileName = path.getParent().resolve(SIGNAL_FILENAME).toAbsolutePath().toString();
        
        try(InputStream is = new FileInputStream(fileName)){
            return new ObjectMapper().readValue(is, TestRunSignal.class);
        }
    }

    /**
     * Save current test run response to registry
     * @param path
     * Path to current test run
     * @param response
     * Portal response for signal upload
     * @throws IOException
     */
    public void savePortalResponse(Path path, String response) throws IOException{
        String filename = path.getParent().resolve(PORTAL_RECORD_FILENAME).toAbsolutePath().toString();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(filename))){
            writer.write(response);
        }
    }

    /**
     * Extract the test run id from path based on drillbit registry specification
     * @param path
     * Path object point to a test run
     * @return
     * Test run id if Path object contains a test run id, otherwise return an empty 
     * string
     */
    public String getTestRunId(Path path){
        return getTestRunId(path.toAbsolutePath().toString());
    }

    /**
     * Extract the test run id from path based on drillbit registry specification
     * @param path
     * String of path to a test run
     * @return
     * Test run id if Path object contains a test run id, otherwise return test run id
     * based on current local time
     */
    public String getTestRunId(String path){
        DateTimeFormatter drillbitDateFormatter = DateTimeFormatter.ofPattern(DRILLBIT_TESTRUN_PATTERN_STRING);
        String testRunId = DRILLBIT_TESTRUN_PREFIX + drillbitDateFormatter.format(OffsetDateTime.now( ZoneOffset.UTC ));
        Pattern pattern = Pattern.compile(".*(TestRun-\\d{8}-\\d{6}).*");
        Matcher matcher = pattern.matcher(path);
        return matcher.find( ) ? matcher.group(0) : testRunId;
    }

}
