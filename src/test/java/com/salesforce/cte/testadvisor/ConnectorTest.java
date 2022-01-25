/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.salesforce.cte.helper.TestAdvisorCipherException;
import com.salesforce.cte.helper.TestAdvisorPortalException;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author gneumann
 * @author Yibing Tao
 */
public class ConnectorTest {

    private Path root;
    private Registry registry;
    private SecretsManager secretsManager;
    
    @Before
    public void setup() throws IOException, TestAdvisorCipherException {
        root = Files.createTempDirectory("testadvisor");
        registry = new Registry(root);
        secretsManager = new SecretsManager(registry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyClientId() throws IOException, TestAdvisorCipherException{
        registry.getRegistryProperties().setProperty("portal.clientid", "");
        registry.saveRegistryProperties();
        new Connector(registry, secretsManager);
    }

    @Test(expected = TestAdvisorPortalException.class)
	public void testApexPostBadRequest() throws Exception {  
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        //and:
        when(builder.build()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("response"));
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);

        //should throw TestAdvisorPortalException 
        registry.saveRegistryProperty("portal.clientid", "123");
        Connector connector = new Connector(registry, secretsManager);
        connector.setBuilder(builder);

        connector.postApex("endpoint", "payload");
	}

    @Test
	public void testApexPostRequest() throws Exception {  
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        //and:
        when(builder.build()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("response"));
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);

        //should throw TestAdvisorPortalException 
        registry.saveRegistryProperty("portal.clientid", "123");
        registry.saveRegistryProperty("portal.url", "https://testadvisor--e2e.my.salesforce.com");
        Connector connector = new Connector(registry, secretsManager);
        connector.setBuilder(builder);

        String response = connector.postApex("endpoint", "payload");
        assertEquals("response", response);
	}


    @Test(expected = TestAdvisorPortalException.class)
    public void testConnectToPortalBadRequest() throws ClientProtocolException, IOException, TestAdvisorCipherException, TestAdvisorPortalException {
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        //and:
        when(builder.build()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("response"));
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);

        //should throw TestAdvisorPortalException 
        registry.saveRegistryProperty("portal.clientid", "123");
        registry.saveRegistryProperty("portal.token.encrypted", "no");
        secretsManager.setRefreshToken("refreshToken");
        Connector connector = new Connector(registry, secretsManager);
        connector.setBuilder(builder);

        connector.connectToPortal();
    }

    @Test
    public void testConnectToPortal() throws ClientProtocolException, IOException, TestAdvisorCipherException, TestAdvisorPortalException {
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        //and:
        when(builder.build()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity("{\"access_token\":\"123\"}"));
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        //should throw TestAdvisorPortalException 
        registry.saveRegistryProperty("portal.clientid", "123");
        registry.saveRegistryProperty("portal.token.encrypted", "no");
        secretsManager.setRefreshToken("refreshToken");
        Connector connector = new Connector(registry, secretsManager);
        connector.setBuilder(builder);

        connector.connectToPortal();
        assertEquals("123", secretsManager.getAccessToken()); 
    }

    @Test
    public void testSetupWithPortal() throws ClientProtocolException, IOException, TestAdvisorPortalException, InterruptedException, TestAdvisorCipherException{
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        CloseableHttpResponse httpResponse2 = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        StatusLine statusLine2 = mock(StatusLine.class);

        //and:
        when(builder.build()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse).thenReturn(httpResponse2);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(new StringEntity(
            "{\"user_code\":\"123\",\"device_code\":\"456\",\"verification_uri\":\"789\",\"interval\":0}"));
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        when(httpResponse2.getStatusLine()).thenReturn(statusLine2);
        when(httpResponse2.getEntity()).thenReturn(new StringEntity(
            "{\"access_token\":\"123\",\"refresh_token\":\"456\",\"instance_url\":\"abc\"}"));
        when(statusLine2.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        registry.saveRegistryProperty("portal.clientid", "123");
        registry.saveRegistryProperty("portal.token.encrypted", "no");
        Connector connector = new Connector(registry, secretsManager);
        connector.setBuilder(builder);

        // MOCK System.in
        String data = " ";
        InputStream stdin = System.in;
        System.setIn(new ByteArrayInputStream(data.getBytes()));
               
        connector.setupConnectionWithPortal();

        // RESTABLISH System.in
        System.setIn(stdin); 

        assertEquals("123", secretsManager.getAccessToken()); 
        assertEquals("456", secretsManager.getRefreshToken()); 
        assertEquals("abc",registry.getRegistryProperties().getProperty("portal.url"));
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
