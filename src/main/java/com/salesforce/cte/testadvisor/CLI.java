/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.salesforce.cte.adapter.TestAdvisorResultAdapter;
import com.salesforce.cte.adapter.TestNGAdapter;
import com.salesforce.cte.datamodel.client.TestRunSignal;
import com.salesforce.cte.helper.TestAdvisorCipherException;
import com.salesforce.cte.helper.TestAdvisorPortalException;
import com.salesforce.cte.helper.ProcessException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * @author Yibing Tao
 * Process command line arguments for testadvisor-cli
 */
public class CLI {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String PORTAL_UPLOAD_ENDPOINT_V1 = "services/apexrest/v1/BSTRun/";
    private static final String VERSION_PROPERTY = "testadvisor.cli.version";

    private String command; //testadvisor cli command, expect to be upper case
    public String getCommand(){
        return this.command;
    }

    private String resultFileName; //testadvisor test result file path, default to registry root
    public String getResultFileName(){
        return resultFileName;
    }
    
    private boolean force; //force to run the commands, default to be false
    public boolean isForce(){
        return force;
    }

    private Registry registry = new Registry(); //testadvisor registry to handle all registry request
    private Connector connector; //connector handle all Protoal communictation
    private SecretsManager secretsManager; // secrets manager handles encryption, decryption and screts storage
    private Processor processor = new Processor(registry);

    private String version;
    public String getVersion(){
        return this.version;
    }
    
    public static void main(String[] args) throws Exception{
        LOGGER.log(Level.INFO, "CLI Starts...");
        CLI cli = new CLI(args);
        if (cli.getCommand()==null) return;
        switch (cli.getCommand()){
            case "SETUP":
                cli.setup();
                break;
            case "PROCESS":
                cli.process();             
                break;
            case "UPLOAD":
                cli.upload();
                break;
            default:
                LOGGER.log(Level.WARNING, "Unknow command:{0}",cli.getCommand());
        }
        LOGGER.log(Level.INFO, "CLI Completed.");
    }

    /**
     * Process command line
     * @param args 
     * command line args
     * @throws ParseException
     * This exception is thrown for parsing command line arguments
     * @throws TestAdvisorCipherException
     * This exception is thrown for any chipher related failure
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public CLI(String[] args) throws IOException, TestAdvisorCipherException, ParseException{
        final Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        version = validateVersion(properties.getProperty(VERSION_PROPERTY,""));
        LOGGER.log(Level.INFO,"CLI version:{0}",version);

        processArgs(args);

        secretsManager = new SecretsManager(registry);
        connector = new Connector(registry, secretsManager);
    }

    private String validateVersion(String versionString){
        return Pattern.matches("\\d+\\.\\d+\\.[0-9a-zA-Z-]+", versionString) ? versionString : "";
    }
    /**
     * 
     * @param args
     * command line arguments
     * @throws ParseException
     */
    private void processArgs(String[] args) throws ParseException{
        Options options = new Options();

        options.addOption(Option.builder("h").longOpt("help").desc("Show TestAdvisor-CLI usage.").build());
        options.addOption(Option.builder("v").longOpt("version").desc("Show TestAdvisor-CLI version.").build());
        options.addOption(Option.builder("n").longOpt("name").hasArg().argName("result file name")
                                .desc("Test result file name.").build());
        options.addOption(Option.builder("c").longOpt("cmd").hasArg().argName("COMMAND")
                                .desc("TestAdvisor-CLI command, Setup|Process|Upload|Download|Clean.").build());
        options.addOption(Option.builder("f").longOpt("force")
                                .desc("Force to run current command while ignore TestAdvisor registry state.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args); 

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("TestAdvisor-CLI", "A command line tool to process test results and manage TestAdvisor registry"
                                ,options,"\nPlease report all issue to cte-tech@salesforce.com",true);
            return;
        }
        if (cmd.hasOption("version")){
            System.out.println(String.format("version:%s",this.version));
            return;
        }

        if (cmd.hasOption("name")){
            resultFileName = cmd.getOptionValue("name");
        }

        this.force = cmd.hasOption("force");
        this.command = cmd.hasOption("cmd") ? cmd.getOptionValue("cmd").toUpperCase() : "UNKNOWN";
    }

    /**
     * Setup TestAdvisor registry and Portal connectiion
     * @throws NoSuchAlgorithmException
     * This exception is thrown when a particular cryptographic algorithm is requested but is not available in the environment.
     * @throws TestAdvisorCipherException
     * This exception is thrown for any chipher related failure
     * @throws InterruptedException
     * This exception is thrown when current thread fails to sleep
     * @throws TestAdvisorPortalException
     * This exception is thrown when testadvisor portal rejects cli payload
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     */
    public void setup() throws NoSuchAlgorithmException, IOException, TestAdvisorPortalException, InterruptedException, TestAdvisorCipherException {
        //connect to portal
        LOGGER.log(Level.INFO,"Setup connection with Portal");
        connector.setupConnectionWithPortal();
    }

    /**
     * Process test results
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     * @throws ProcessException
     * This exception is thrown when it fails to process test result
     */
    public void process() throws IOException, ProcessException {
        //process test results and save the signal file
        LOGGER.log(Level.INFO,"Process test result");
        TestRunSignal testRunSignal = registry.getTestRunProperties();
        
        if (resultFileName == null || resultFileName.isEmpty()){
            for(Path path : registry.getUnprocessedTestRunList()){
                LOGGER.log(Level.INFO,"Processing {0}", path);
                testRunSignal.testRunId = registry.getTestRunId(path);
                processTestAdvisorFile(registry.getTestAdvisorTestResultFile(path),testRunSignal);
                registry.saveTestRunSignal(testRunSignal);
            }
        }else{
            LOGGER.log(Level.INFO,"Processing {0}", resultFileName);
            testRunSignal.testRunId = registry.getTestRunId(resultFileName);
            processFile(resultFileName, testRunSignal);
            registry.saveTestRunSignal(testRunSignal);
        }
    }

    /**
     * Process a single TestNG test result file
     * @param file
     * file name to the test result file
     * @param testRunSignal
     * test run signal object contains test result
     * @throws FileNotFoundException
     * This exception is thrown when it failed to access test result file
     * @throws ProcessException
     * This exception is thrown when it fails to process test result
     */
    private void processFile(String file, TestRunSignal testRunSignal) throws IOException, ProcessException {
        if (!new File(file).exists() && !new File(file).canRead())
            return;
        try(InputStream is = new FileInputStream(file)){
            processor.process(is, testRunSignal, new TestNGAdapter());
        }
    }

    /**
     * Process a single TestAdvisor lib test result file
     * @param file
     * file name to the test result file
     * @param testRunSignal
     * test run signal object contains test result
     * @throws FileNotFoundException
     * This exception is thrown when it failed to access test result file
     * @throws ProcessException
     * This exception is thrown when it fails to process test result
     */
    private void processTestAdvisorFile(Path filePath, TestRunSignal testRunSignal) throws IOException, ProcessException{
        if (filePath == null || !filePath.toFile().exists() || !filePath.toFile().canRead())
            return;
        try(InputStream is = new FileInputStream(filePath.toString())){
            processor.process(is, testRunSignal, new TestAdvisorResultAdapter());
        }
    }

    /**
     * Upload test run signals
     * @throws TestAdvisorPortalException
     * This exception is thrown when TestAdvisor portal rejects cli payload.
     * @throws IOException
     * This exception is thrown when it failed to access registry properties
     * @throws TestAdvisorCipherException
     * This exception is thrown for any chipher related failure
     */
    public void upload() throws IOException, TestAdvisorPortalException, TestAdvisorCipherException {
        //upload test run signals to portal
        LOGGER.log(Level.INFO,"Upload test signals");
        connector.connectToPortal();    
        for(Path path : registry.getReadyToUploadTestRunList()){
            LOGGER.log(Level.INFO,"Uploading {0}", path);
            String response = connector.postApex(PORTAL_UPLOAD_ENDPOINT_V1, new String(Files.readAllBytes(path)));
            registry.savePortalResponse(path, response);
        }
    }
}
