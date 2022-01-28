/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.salesforce.cte.helper.TestAdvisorCipherException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SecretsManagerTest {
    
    private Path root;
    private Registry registry;
    
    @Before
    public void setup() throws IOException, TestAdvisorCipherException{
        root = Files.createTempDirectory("testadvisor");
        registry = new Registry(root);
    }

    @Test
    public void noEncryptTest() throws IOException, TestAdvisorCipherException {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=no\n");
            writer.write("portal.accesstoken=\n");
            writer.write("portal.refreshtoken=\n");
        }
        registry.loadRegistryProperties();
        
        SecretsManager manager = new SecretsManager(registry);
       
        assertUnEncryptedToken(manager);
    }

    @Test
    public void encryptTest() throws IOException, TestAdvisorCipherException, NoSuchAlgorithmException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=yes\n");
            writer.write("portal.accesstoken=abc\n");
            writer.write("portal.refreshtoken=xyz\n");
        }
        registry.loadRegistryProperties();

        SecretsManager manager1 = new SecretsManager(registry);
        SecretsManager manager2 = new SecretsManager(registry);
        assertEncryptedToken(manager1,manager2);
    }

    @Test
    public void encryptDefaultTest() throws IOException, TestAdvisorCipherException, NoSuchAlgorithmException, PasswordAccessException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=yes\n");
            writer.write("portal.accesstoken=abc\n");
            writer.write("portal.refreshtoken=xyz\n");
        }
        registry.loadRegistryProperties();
        
        try {
            Keyring keyring = Keyring.create();
            keyring.deletePassword("TestAdvisor", "secretkey");
            SecretsManager manager1 = new SecretsManager(registry,keyring);
            SecretsManager manager2 = new SecretsManager(registry,keyring);
        assertEncryptedToken(manager1,manager2);
        } catch (BackendNotSupportedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void mockKeyingEncryptTest() throws IOException, TestAdvisorCipherException, PasswordAccessException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=yes\n");
            writer.write("portal.accesstoken=abc\n");
            writer.write("portal.refreshtoken=xyz\n");
        }
        registry.loadRegistryProperties();

        String encodedKey = "O7vS3cpKnz1pS1HROimySxvoOdgYiI1kzUkbV+0uWVg=";
        Keyring keyring = mock(Keyring.class);
        when(keyring.getPassword(anyString(), anyString())).thenReturn(encodedKey);
        SecretsManager manager1 = new SecretsManager(registry,keyring);
        SecretsManager manager2 = new SecretsManager(registry,keyring);

        assertEncryptedToken(manager1, manager2);
    }

    private void assertEncryptedToken(SecretsManager manager1, SecretsManager manager2) throws IOException, TestAdvisorCipherException{
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        manager1.setAccessToken(accessToken);
        registry.loadRegistryProperties();
        assertEquals(accessToken,manager2.getAccessToken());
        assertNotEquals(registry.getRegistryProperties().getProperty("portal.accesstoken"),manager2.getAccessToken());
        manager2.setRefreshToken(refreshToken);
        registry.loadRegistryProperties();
        assertEquals(refreshToken,manager1.getRefreshToken());
        assertNotEquals(registry.getRegistryProperties().getProperty("portal.refreshtoken"),manager1.getRefreshToken());
    }

    private void assertUnEncryptedToken(SecretsManager manager) throws IOException, TestAdvisorCipherException{
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        manager.setAccessToken(accessToken);
        registry.loadRegistryProperties();
        assertEquals(accessToken,manager.getAccessToken());
        assertEquals(registry.getRegistryProperties().getProperty("portal.accesstoken"),manager.getAccessToken());
        manager.setRefreshToken(refreshToken);
        registry.loadRegistryProperties();
        assertEquals(refreshToken,manager.getRefreshToken());
        assertEquals(registry.getRegistryProperties().getProperty("portal.refreshtoken"),manager.getRefreshToken());
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
