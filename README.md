# Drillbit-CLI

Drillbit CLI is a command line tool to which response for process test result, upload test signals and download server signals. This tool will be triggered after test execution and test result generated in a separated process. 

## Command line
> java -jar drillbit-cli.jar [--version] [--help] -c <path> <command> [--force] [--args]

Supported command
* authorize
* process
* upload
* download
* clean

If path option was skipped, the client will first search registry root folder, which is defined in environment variable DRILLBIT_REGISTRY. If not found, the client will use current folder to search.

If force option was provided, the client will ignore the previous command output and rerun the command anyway. 

### Authorize
The authorize command will initiate the authorization process to authorize the CLI to the Portal.
### Process
Process command will find all un-processed test result file recursively from the path, collect all client side signals and write back the signal file along with the test result file.
### Upload
Upload command will upload all un-processed test signal files and saved record id if upload succeed.
### Download
Download command will download all un-downloaded test execution server signal from portal and saved to server signal file.
### Clean
Clean command will clean up local Drillbit registry



