package com.salesforce.cqe.drillbit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

public class RegistryTest {
    
    @Test
    public void registryPropertiesTest() throws IOException{
        Registry registry = new Registry();
        Properties regProperties = registry.getRegistryProperties();

        assertEquals("02957862-f1d9-475a-8ce0-d7ec128ccf42", regProperties.getProperty("ClientRegistryGuid"));
        assertEquals("CS46", regProperties.getProperty("SandboxInstance"));
        assertEquals("00D9A0000009HXt", regProperties.getProperty("SandboxOrgId"));
        assertEquals("PleaseKeep", regProperties.getProperty("SandboxOrgName"));
        assertEquals("Schneider-Sales-Clone", regProperties.getProperty("TestSuiteName"));
    }

    
}
