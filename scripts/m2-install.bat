@echo off
if [%1]==[] goto :usage

echo "Installing testadvisor-cli-%1"
call mvn install:install-file -Dfile=testadvisor-cli-%1.pom -DgroupId=com.salesforce.cte -DartifactId=testadvisor-cli -Dversion=%1 -Dpackaging=pom

call mvn install:install-file -Dfile=testadvisor-cli-%1.jar -DgroupId=com.salesforce.cte -DartifactId=testadvisor-cli -Dsources=testadvisor-cli-%1-sources.jar -Djavadoc=testadvisor-cli-%1-javadoc.jar -Dversion=%1 -Dpackaging=jar 
goto :done

:usage
echo m2-install.bat version

:done