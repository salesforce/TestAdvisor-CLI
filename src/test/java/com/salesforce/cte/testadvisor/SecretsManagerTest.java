/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.salesforce.cte.helper.TestAdvisorCipherException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SecretsManagerTest {
    
    private Path root;
    
    @Before
    public void setup() throws IOException, TestAdvisorCipherException{
        root = Files.createTempDirectory("testadvisor");
    }

    @Test
    public void noEncryptTest() throws IOException, TestAdvisorCipherException {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=no\n");
            writer.write("portal.accesstoken=\n");
            writer.write("portal.refreshtoken=\n");
        }
        SecretsManager manager = new SecretsManager(new Registry(root));
       
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        manager.setAccessToken(accessToken);
        assertEquals(accessToken,manager.getAccessToken());
        manager.setRefreshToken(refreshToken);
        assertEquals(refreshToken,manager.getRefreshToken());
    }

    @Test
    public void encryptTest() throws IOException, TestAdvisorCipherException, NoSuchAlgorithmException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=yes\n");
            writer.write("portal.accesstoken=abc\n");
            writer.write("portal.refreshtoken=xyz\n");
        }
        SecretsManager manager = new SecretsManager(new Registry(root),"passphase");
        assertNotNull(manager);
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        manager.setAccessToken(accessToken);
        assertEquals(accessToken,manager.getAccessToken());
        manager.setRefreshToken(refreshToken);
        assertEquals(refreshToken,manager.getRefreshToken());
    }

    @Test
    public void encryptDefaultTest() throws IOException, TestAdvisorCipherException, NoSuchAlgorithmException{
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("testadvisor.properties").toFile()))){
            writer.write("portal.token.encrypted=yes\n");
            writer.write("portal.accesstoken=abc\n");
            writer.write("portal.refreshtoken=xyz\n");
        }
        SecretsManager manager = new SecretsManager(new Registry(root));
        assertNotNull(manager);
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        manager.setAccessToken(accessToken);
        assertEquals(accessToken,manager.getAccessToken());
        manager.setRefreshToken(refreshToken);
        assertEquals(refreshToken,manager.getRefreshToken());
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
