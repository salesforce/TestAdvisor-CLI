package com.salesforce.cqe.drillbit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import com.salesforce.cqe.helper.DrillbitCipherException;
import com.salesforce.cqe.helper.DrillbitPortalException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
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
    public void setup() throws IOException, DrillbitCipherException {
        root = Files.createTempDirectory("drillbit");
        registry = new Registry(root);
        secretsManager = new SecretsManager(registry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyClientId() throws IOException, DrillbitCipherException{
        new Connector(registry, secretsManager);
    }

    @Test(expected = DrillbitPortalException.class)
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

        //should throw DrillbitPortalException 
        registry.saveRegistryProperty("portal.clientid", "123");
        Connector connector = new Connector(registry, secretsManager);
        connector.setBuilder(builder);

        connector.postApex("endpoint", "payload");
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
