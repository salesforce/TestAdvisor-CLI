package com.salesforce.cqe.drillbit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.salesforce.cqe.helper.SecretsManager;

import org.junit.Test;

public class SecretsManagerTest {
    
    @Test
    public void noEncryptTest() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, 
                    InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException, URISyntaxException{
        URL resource = SecretsManagerTest.class.getResource("/configuration/credentials_noencrypt.properties");
        SecretsManager manager = new SecretsManager(Paths.get(resource.toURI()).toFile().getAbsolutePath());
       
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        manager.setAccessToken(accessToken);
        assertEquals(accessToken,manager.getAccessToken());
        manager.setRefreshToken(refreshToken);
        assertEquals(refreshToken,manager.getRefreshToken());

    }
}
