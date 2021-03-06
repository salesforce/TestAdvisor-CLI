# TestAdvisor-CLI

TestAdvisor CLI is a command line tool to which response for process test result, upload test signals and download server signals. This tool will be triggered after test execution and test result generated in a separated process.

## Command line usage

```
usage: TestAdvisor-CLI [-c <COMMAND>] [-f] [-h] [-n <result file name>] [-v]
A command line tool to process test results and manage TestAdvisor registry
 -c,--cmd <COMMAND>             TestAdvisor-CLI command,
                                Setup|Process|Upload|Download|Clean.
 -f,--force                     Force to run current command while ignore
                                TestAdvisor registry state.
 -h,--help                      Show TestAdvisor-CLI usage.
 -n,--name <result file name>   Test result file name.
 -v,--version                   Show TestAdvisor-CLI version.

Please report all issues to cqe-us@salesforce.com
```

Supported command

* setup
* process
* upload
* download
* clean

If path option was skipped, the client will first search registry root folder, which is defined in environment variable TESTADVISOR_REGISTRY. If not found, the client will use current folder to search.

If force option was provided, the client will ignore the previous command output and rerun the command anyway.

### Setup

The setup command will setup TestAdvisor registry and connect to portal

### Process

Process command will find all un-processed test result file recursively from the path, collect all client side signals and write back the signal file along with the test result file.

### Upload

Upload command will upload all un-processed test signal files and saved record id if upload succeed.

### Clean

Clean command will clean up local TestAdvisor registry

## How to publish to Nexus

1. mvn clean package
   This will ensure your module build and package correctly.
2. mvn release:prepare
   This will prepare the release, more doc here:
   http://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html
   You may choose your release version, the default will only a minor version upgrade.
3. mvn release:perform
   This step will build/publish to nexus and also tag release on git repository, more doc here
   (http://maven.apache.org/maven-release/maven-release-plugin/examples/perform-release.html)
4. mvn release:clean
   Cleans up your local env

### Documenting a new release version

1. Go to the Releases [section](https://git.soma.salesforce.com/cqe/DrillBit-CLI/releases)
2. Click on the "Draft a new release" button
3. Enter the "tag version" and "Release title" value as testadvisor-cli-v0.1.\<new-version\>
4. Write one or more sentences about the changes pushed ([example](https://git.soma.salesforce.com/cqe/DrillBit-Lib/releases/tag/testadvisor-cli-v0.1.\<new-version\>)) and publish the release.
