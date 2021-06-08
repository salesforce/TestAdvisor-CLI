package com.salesforce.cqe.drillbit;

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

    private String command; //drillbit cli command, expect to be upper case
    public String getCommand(){
        return this.command;
    }

    private String resultFilePath; //drillbit test result file path, default to registry root
    public String getPath(){
        return resultFilePath;
    }
    
    private boolean force; //force to run the commands, default to be false
    public boolean isForce(){
        return force;
    }
    public static void main(String[] args) throws Exception{
        
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
                System.out.println("Unknow command:"+cli.getCommand());
        }
    }

    /**
     * Process command line
     * @param args command line args
     * @throws ParseException
     */
    public CLI(String[] args) throws ParseException{
        Options options = new Options();

        options.addOption(Option.builder("h").longOpt("help").desc("Show Drillbit-CLI usage.").build());
        options.addOption(Option.builder("v").longOpt("version").desc("Show Drillbit-CLI version.").build());
        options.addOption(Option.builder("p").longOpt("path").hasArg().argName("PATH TO RESULT")
                                .desc("Path to test result files.\nDefault will be Drillbit registry root.").build());
        options.addOption(Option.builder("c").longOpt("command").hasArg().argName("COMMAND")
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
            System.out.println("version:"+CLI.class.getClass().getPackage().getImplementationVersion());
            return;
        }

        // The default location for test result file will be drillbit registry or current folder
        this.resultFilePath = System.getProperty("DRILLBIT_REGISTRY",System.getProperty("user.dir"));
        if (cmd.hasOption("path")){
            resultFilePath = cmd.getOptionValue("path");
        }

        this.force = cmd.hasOption("force");
        this.command = cmd.hasOption("command") ? cmd.getOptionValue("command").toUpperCase() : "UNKNOWN";
    }
    /**
     * Setup Drillbit registry and Portal connectiion
     * @throws Exception
     */
    public void setup(){
        //drillbit registry setup and connect to portal
    }

    /**
     * Process test results
     */
    public void process() {
        //process test results and save the signal file
    }

    /**
     * Upload test run signals
     */
    public void upload() {
        //upload test run signals to portal
    }
}
