package com.salesforce.cqe.drillbit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.salesforce.cqe.helper.DrillbitCipherException;
import com.salesforce.cqe.helper.SecretsManager;

import org.junit.Test;

public class SecretsManagerTest {
    
    @Test
    public void noEncryptTest() throws IOException, DrillbitCipherException {
        Path root = Files.createTempDirectory("drillbit");
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("drillbit.properties").toFile()))){
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

        Files.deleteIfExists(root.resolve("drillbit.properties"));
        Files.deleteIfExists(root);
    }
}
