package com.salesforce.cqe.drillbit;

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

import com.salesforce.cqe.adapter.TestNGAdapter;
import com.salesforce.cqe.datamodel.client.TestRunSignal;
import com.salesforce.cqe.helper.DrillbitCipherException;
import com.salesforce.cqe.helper.DrillbitPortalException;
import com.salesforce.cqe.helper.ProcessException;
import com.salesforce.cqe.helper.SecretsManager;
import com.salesforce.cqe.processor.Processor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * @author Yibing Tao
 * Process command line arguments for drillbit-cli
 */
public class CLI {

    private static final Logger LOGGER = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );

    private static final String PORTAL_UPLOAD_ENDPOINT_V1 = "services/apexrest/v1/BSTRun/";

    private String command; //drillbit cli command, expect to be upper case
    public String getCommand(){
        return this.command;
    }

    private String resultFileName; //drillbit test result file path, default to registry root
    public String getResultFileName(){
        return resultFileName;
    }
    
    private boolean force; //force to run the commands, default to be false
    public boolean isForce(){
        return force;
    }

    private Registry registry = new Registry(); //drillbit registry to handle all registry request
    private Connector connector; //connector handle all Protoal communictation
    private SecretsManager secretsManager; // secrets manager handles encryption, decryption and screts storage

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
     * @throws DrillbitCipherException
     * @throws IOException
     */
    public CLI(String[] args) throws IOException, DrillbitCipherException, ParseException{
        Options options = new Options();

        options.addOption(Option.builder("h").longOpt("help").desc("Show Drillbit-CLI usage.").build());
        options.addOption(Option.builder("v").longOpt("version").desc("Show Drillbit-CLI version.").build());
        options.addOption(Option.builder("n").longOpt("name").hasArg().argName("result file name")
                                .desc("test result file name.\n Default will be Drillbit registry root.").build());
        options.addOption(Option.builder("c").longOpt("cmd").hasArg().argName("COMMAND")
                                .desc("Drillbit-CLI command, Setup|Process|Upload|Download|Clean.").build());
        options.addOption(Option.builder("f").longOpt("force")
                                .desc("Force to run current command while ignore Drillbit registry state.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args); 

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Drillbit-CLI", "A command line tool to process test results and manage Drillbit registry"
                                ,options,"\nPlease report all issue to cqe-us@salesforce.com",true);
            return;
        }
        if (cmd.hasOption("version")){
            LOGGER.log(Level.INFO,"version:{0}",CLI.class.getClass().getPackage().getImplementationVersion());
            return;
        }

        // The default location for test result file will be drillbit registry or current folder
        this.resultFileName = System.getProperty("DRILLBIT_REGISTRY",System.getProperty("user.dir"));
        if (cmd.hasOption("name")){
            resultFileName = cmd.getOptionValue("name");
        }

        this.force = cmd.hasOption("force");
        this.command = cmd.hasOption("cmd") ? cmd.getOptionValue("cmd").toUpperCase() : "UNKNOWN";

        secretsManager = new SecretsManager(registry);
        connector = new Connector(registry, secretsManager);

    }
    /**
     * Setup Drillbit registry and Portal connectiion
     * @throws NoSuchAlgorithmException
     * @throws DrillbitCipherException
     * @throws InterruptedException
     * @throws DrillbitPortalException
     * @throws IOException
     */
    public void setup() throws NoSuchAlgorithmException, IOException, DrillbitPortalException, InterruptedException, DrillbitCipherException {
        //drillbit registry setup and connect to portal
        if (secretsManager.isSetupRequired())
            connector.setupConnectionWithPortal();
    }

    /**
     * Process test results
     * @throws IOException
     * @throws ProcessException
     */
    public void process() throws IOException, ProcessException {
        //process test results and save the signal file
        TestRunSignal testRunSignal = new TestRunSignal();
        Properties prop = registry.getRegistryProperties();
        testRunSignal.sandboxInstance = prop.getProperty("SandboxInstance");
        testRunSignal.sandboxOrgId = prop.getProperty("SandboxOrgId");
        testRunSignal.sandboxOrgName = prop.getProperty("SandboxOrgName");
        testRunSignal.clientRegistryGuid =  UUID.fromString(prop.getProperty("ClientRegistryGuid"));
        testRunSignal.testSuiteName = prop.getProperty("TestSuiteName");
        testRunSignal.clientBuildId = System.getenv("CLIENT_BUILD_ID");
        
        if (resultFileName == null || resultFileName.isEmpty()){
            for(Path path : registry.getUnprocessedTestRunList()){
                testRunSignal.testRunId = registry.getTestRunId(path);
                processFile(path.toString(),testRunSignal);
                registry.saveTestRunSignal(testRunSignal);
            }
        }else{
            testRunSignal.testRunId = registry.getTestRunId(resultFileName);
            processFile(resultFileName, testRunSignal);
            registry.saveTestRunSignal(testRunSignal);
        }
    }

    /**
     * Process a single test result file
     * @param file
     * file name to the test result file
     * @param testRunSignal
     * @throws FileNotFoundException
     * @throws ProcessException
     */
    private void processFile(String file, TestRunSignal testRunSignal) throws IOException, ProcessException {
        try(InputStream is = new FileInputStream(file)){
            Processor.process(is, testRunSignal, new TestNGAdapter());
        }
    }

    /**
     * Upload test run signals
     * @throws DrillbitPortalException
     * @throws IOException
     * @throws DrillbitCipherException
     */
    public void upload() throws IOException, DrillbitPortalException, DrillbitCipherException {
        //upload test run signals to portal
        connector.connectToPortal();    
        for(Path path : registry.getReadyToUploadTestRunList()){
            String response = connector.postApex(PORTAL_UPLOAD_ENDPOINT_V1, new String(Files.readAllBytes(path)));
            registry.savePortalResponse(path, response);
        }
    }
}
