#!/bin/bash
if [ -z "$1" ]
  then
    echo "Usage: m2-install.sh <version>"
    exit 0
fi

echo "Installing testadvisor-cli-$1"
mvn install:install-file -Dfile=testadvisor-cli-$1.pom \
            -DgroupId=com.salesforce.cte \
            -DartifactId=testadvisor-cli \
            -Dversion=$1 \
            -Dpackaging=pom 

mvn install:install-file -Dfile=testadvisor-cli-$1.jar \
            -DgroupId=com.salesforce.cte \
            -DartifactId=testadvisor-cli \
            -Dsources=testadvisor-cli-$1-sources.jar \
            -Djavadoc=testadvisor-cli-$1-javadoc.jar \
            -Dversion=$1 \
            -Dpackaging=jar 





