package com.salesforce.cqe.drillbit;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegistryTest {
    
    private Path root;
    
    @Before
    public void setup() throws IOException {
        root = Files.createTempDirectory("drillbit");
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("drillbit.properties").toFile()))){
            writer.write("portal.token.encrypted=no\n");
            writer.write("portal.accesstoken=\n");
            writer.write("portal.refreshtoken=\n");
            writer.write("portal.clientid=\n");
            writer.write("portal.url=\n");
            writer.write("ClientRegistryGuid=02957862-f1d9-475a-8ce0-d7ec128ccf42\n");
            writer.write("TestSuiteName=Schneider-Sales-Clone\n");
            writer.write("SandboxInstance=CS46\n");
            writer.write("SandboxOrgName=PleaseKeep\n");
            writer.write("SandboxOrgId=00D9A0000009HXt\n");
            writer.write("auth.url=\n");
        }
    }

    @Test
    public void registryPropertiesTest() throws IOException{      
        Registry registry = new Registry(root);
        Properties regProperties = registry.getRegistryProperties();        

        assertEquals("02957862-f1d9-475a-8ce0-d7ec128ccf42", regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("CS46", regProperties.getProperty("SandboxInstance"));
        assertEquals("00D9A0000009HXt", regProperties.getProperty("SandboxOrgId"));
        assertEquals("PleaseKeep", regProperties.getProperty("SandboxOrgName"));
        assertEquals("Schneider-Sales-Clone", regProperties.getProperty("TestSuiteName"));
    }

    @After
    public void teardown() throws IOException{
        Files.deleteIfExists(root.resolve("drillbit.properties"));
        Files.deleteIfExists(root);
    }
    
}
