# Drillbit-CLI

Drillbit CLI is a command line tool to which response for process test result, upload test signals and download server signals. This tool will be triggered after test execution and test result generated in a separated process. 

## Command line usage
```
usage: Drillbit-CLI [-c <COMMAND>] [-f] [-h] [-n <result file name>] [-v]
A command line tool to process test results and manage Drillbit registry
 -c,--cmd <COMMAND>             Drillbit-CLI command,
                                Setup|Process|Upload|Download|Clean.
 -f,--force                     Force to run current command while ignore
                                Drillbit registry state.
 -h,--help                      Show Drillbit-CLI usage.
 -n,--name <result file name>   Test result file name.
 -v,--version                   Show Drillbit-CLI version.

Please report all issues to cqe-us@salesforce.com
```
Supported command
* setup
* process
* upload
* download
* clean

If path option was skipped, the client will first search registry root folder, which is defined in environment variable DRILLBIT_REGISTRY. If not found, the client will use current folder to search.

If force option was provided, the client will ignore the previous command output and rerun the command anyway. 

### Setup
The setup command will setup drillbit registry and connect to portal
### Process
Process command will find all un-processed test result file recursively from the path, collect all client side signals and write back the signal file along with the test result file.
### Upload
Upload command will upload all un-processed test signal files and saved record id if upload succeed.
### Download
Download command will download all un-downloaded test execution server signal from portal and saved to server signal file.
### Clean
Clean command will clean up local Drillbit registry



