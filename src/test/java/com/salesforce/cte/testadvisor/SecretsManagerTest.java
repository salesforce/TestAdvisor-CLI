/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.salesforce.cte.helper.TestAdvisorCipherException;

import org.junit.Test;

public class SecretsManagerTest {
    
    @Test
    public void noEncryptTest() throws IOException, TestAdvisorCipherException {
        Path root = Files.createTempDirectory("testadvisor");
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

        Files.deleteIfExists(root.resolve("testadvisor.properties"));
        Files.deleteIfExists(root);
    }
}
