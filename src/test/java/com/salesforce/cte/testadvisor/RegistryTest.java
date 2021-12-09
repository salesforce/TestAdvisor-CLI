/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import com.salesforce.cte.common.TestAdvisorResult;
import com.salesforce.cte.datamodel.client.TestRunSignal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegistryTest {
    
    private Path root;
    
    @Before
    public void setup() throws IOException {
        root = Files.createTempDirectory("testadvisor");
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

        assertEquals(root,registry.getRegistryRoot());
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

    @Test
    public void testGetUnprocessedList() throws IOException{
        //setup
        Registry registry = new Registry(root);
        
        Path testrun = RegistryHelper.createTestRun(registry, 0);
        Files.createFile(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun.resolve(Registry.SIGNAL_FILENAME));
        Path testrun2 = RegistryHelper.createTestRun(registry, 2);
        Files.createFile(testrun2.resolve(Registry.TESTADVISOR_TEST_RESULT));

        //test
        List<Path> list = registry.getUnprocessedTestRunList();
        assertEquals(1, list.size());
        assertEquals(0,list.get(0).compareTo(testrun2));
    }

    @Test
    public void testGetReadyToUploadList() throws IOException{
        //setup
        Registry registry = new Registry(root);
        Path testrun = RegistryHelper.createTestRun(registry,0);
        Files.createFile(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun.resolve(Registry.SIGNAL_FILENAME));
        Path testrun2 = RegistryHelper.createTestRun(registry,2);
        Files.createFile(testrun2.resolve(Registry.PORTAL_RECORD_FILENAME));
        Files.createFile(testrun2.resolve(Registry.TESTADVISOR_TEST_RESULT));

        //test
        List<Path> list = registry.getReadyToUploadTestRunList();
        assertEquals(1, list.size());
        assertEquals(0,list.get(0).toAbsolutePath()
            .compareTo(testrun.resolve(Registry.SIGNAL_FILENAME).toAbsolutePath()));
    }

    @Test
    public void testSaveTestRunSignal() throws IOException{
        Registry registry = new Registry(root);

        Path testrun = RegistryHelper.createTestRun(registry,0);
        String testrunId = registry.getTestRunId(testrun);

        TestRunSignal testRunSignal = RegistryHelper.createTestRunSignal(testrunId);

        String filename = registry.saveTestRunSignal(testRunSignal);

        TestRunSignal testRunSignal2 = registry.getTestRunSignal(Paths.get(filename));

        assertEquals(testRunSignal.clientRegistryGuid, testRunSignal2.clientRegistryGuid);
    }

    @Test
    public void testSaveResponse() throws IOException{
        Registry registry = new Registry(root);
        Path testrun = RegistryHelper.createTestRun(registry,0);

        String text = "some response";
        registry.savePortalResponse(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT), text);

        List<String> responses = Files.readAllLines(testrun.resolve(Registry.PORTAL_RECORD_FILENAME));
        
        assertEquals(1, responses.size());
        assertEquals(text, responses.get(0));

    }

    @Test
    public void testGetTestResultList() throws IOException{
        //setup
        Registry registry = new Registry(root);
        Path testrun = RegistryHelper.createTestRun(registry,0);
        Files.createFile(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun.resolve(Registry.PORTAL_RECORD_FILENAME));

        //test
        Path path = registry.getTestAdvisorTestResultFile(testrun);
        assertEquals(0,path.toAbsolutePath()
            .compareTo(testrun.resolve(Registry.TESTADVISOR_TEST_RESULT).toAbsolutePath()));
    }

    @Test
    public void testGetTestRunId() throws IOException{
        Registry registry = new Registry(root);
        Path testrun1 = RegistryHelper.createTestRun(registry,0);

        //test
        String testrunId = registry.getTestRunId(testrun1);

        assertEquals(testrun1.getName(testrun1.getNameCount()-1).toString(), testrunId);
    }

    @Test
    public void testGetAllTestRunList() throws IOException {
        Registry registry = new Registry(root);
        Path testrun1 = RegistryHelper.createTestRun(registry,0);
        Path testrun2 = RegistryHelper.createTestRun(registry,1000);
        Path testrun3 = RegistryHelper.createTestRun(registry,300);
        Files.createFile(testrun1.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun2.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun3.resolve(Registry.TESTADVISOR_TEST_RESULT));

        //test
        List<Path> allTestRunList = registry.getAllTestRuns();

        assertEquals(3,allTestRunList.size());
        assertEquals(testrun2.toString(), allTestRunList.get(0).toString());
        assertEquals(testrun3.toString(), allTestRunList.get(1).toString());
        assertEquals(testrun1.toString(), allTestRunList.get(2).toString());
    }

    @Test
    public void testGetAllTestRunListFilterEmpty() throws IOException {
        Registry registry = new Registry(root);
        Path testrun1 = RegistryHelper.createTestRun(registry,0);
        RegistryHelper.createTestRun(registry,1000);
        Path testrun3 = RegistryHelper.createTestRun(registry,300);
        Files.createFile(testrun1.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun3.resolve(Registry.TESTADVISOR_TEST_RESULT));

        //test
        List<Path> allTestRunList = registry.getAllTestRuns();

        assertEquals(2,allTestRunList.size());
        assertEquals(testrun3.toString(), allTestRunList.get(0).toString());
        assertEquals(testrun1.toString(), allTestRunList.get(1).toString());
    }

    @Test
    public void testFindBeforeTestRunList() throws IOException{
        Registry registry = new Registry(root);
        Path testrun1 = RegistryHelper.createTestRun(registry,0);
        Path testrun2 = RegistryHelper.createTestRun(registry,1000);
        Path testrun3 = RegistryHelper.createTestRun(registry,300);
        Files.createFile(testrun1.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun2.resolve(Registry.TESTADVISOR_TEST_RESULT));
        Files.createFile(testrun3.resolve(Registry.TESTADVISOR_TEST_RESULT));
        registry.getAllTestRuns();
        
        //test
        List<Path> beforeList = registry.findBeforeTestRunList(testrun1);
        assertEquals(0,beforeList.size());

        beforeList = registry.findBeforeTestRunList(testrun3);
        assertEquals(1,beforeList.size());
        assertEquals(testrun1.toString(), beforeList.get(0).toString());

        beforeList = registry.findBeforeTestRunList(testrun2);
        assertEquals(2,beforeList.size());
        assertEquals(testrun3.toString(), beforeList.get(0).toString());
        assertEquals(testrun1.toString(), beforeList.get(1).toString());
    }

    @Test
    public void testGetBaseline() throws IOException{
        Registry registry = new Registry(root);

        Path testrun1 = RegistryHelper.createTestRun(registry,0);
        RegistryHelper.saveTestAdvisorResult(testrun1, RegistryHelper.createTestAdvisorResult());

        Path testrun2 = RegistryHelper.createTestRun(registry,1000);
        RegistryHelper.saveTestAdvisorResult(testrun2, RegistryHelper.createTestAdvisorResult());

        registry.getAllTestRuns();
        Path baseline = registry.getBaselineTestRun(testrun2, "testcasePass");
        assertEquals(testrun1, baseline);
    }

    @Test
    public void testGetNotExistTestResult() throws IOException{
        Registry registry = new Registry(root);

        Path testrun1 = RegistryHelper.createTestRun(registry,0);
        TestAdvisorResult result = registry.getTestAdvisorResult(testrun1);
        assertEquals(0, result.testCaseExecutionList.size());

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
