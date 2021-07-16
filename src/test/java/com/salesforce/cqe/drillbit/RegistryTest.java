package com.salesforce.cqe.drillbit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
    }

    @Test
    public void registryPropertiesTest() throws IOException{      
        Registry registry = new Registry(root);
        Properties regProperties = registry.getRegistryProperties();   
        regProperties.setProperty("ClientRegistryGuid", "02957862-f1d9-475a-8ce0-d7ec128ccf42");     
        regProperties.setProperty("SandboxInstance", "CS46");
        regProperties.setProperty("SandboxOrgId", "00D9A0000009HXt");
        regProperties.setProperty("SandboxOrgName", "Pleasekeep");
        regProperties.setProperty("TestSuiteName", "Schneider-Sales-Clone");

        assertEquals("02957862-f1d9-475a-8ce0-d7ec128ccf42", regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("CS46", regProperties.getProperty("SandboxInstance"));
        assertEquals("00D9A0000009HXt", regProperties.getProperty("SandboxOrgId"));
        assertEquals("Pleasekeep", regProperties.getProperty("SandboxOrgName"));
        assertEquals("Schneider-Sales-Clone", regProperties.getProperty("TestSuiteName"));
    }

    @Test
    public void registryPropertiesSaveTest() throws IOException{
        Registry registry = new Registry(root);
        registry.saveRegistryProperty("ClientRegistryGuid", "02957862-f1d9-475a-8ce0-d7ec128ccf42");     
        registry.saveRegistryProperty("SandboxInstance", "CS46");
        registry.saveRegistryProperty("SandboxOrgId", "00D9A0000009HXt");
        registry.saveRegistryProperty("SandboxOrgName", "Pleasekeep");
        registry.saveRegistryProperty("TestSuiteName", "Schneider-Sales-Clone");

        Properties regProperties = registry.getRegistryProperties();   
        assertEquals("02957862-f1d9-475a-8ce0-d7ec128ccf42", regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("CS46", regProperties.getProperty("SandboxInstance"));
        assertEquals("00D9A0000009HXt", regProperties.getProperty("SandboxOrgId"));
        assertEquals("Pleasekeep", regProperties.getProperty("SandboxOrgName"));
        assertEquals("Schneider-Sales-Clone", regProperties.getProperty("TestSuiteName"));
    }

    @Test
    public void createRegistryPropertiesTest() throws IOException {
        Registry registry = new Registry(root.resolve("subfolder").resolve("other"));
        Properties regProperties = registry.getRegistryProperties();    
        assertNotNull(regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("no", regProperties.getProperty("portal.token.encrypted"));
        assertTrue(regProperties.getProperty("portal.refreshtoken").isEmpty());
    }

    @After
    public void teardown() throws IOException{
        removeDirectory(root.toFile());
    }

    private void removeDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }
}
