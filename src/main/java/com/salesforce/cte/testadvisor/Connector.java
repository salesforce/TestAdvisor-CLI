/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.salesforce.cte.helper.TestAdvisorCipherException;
import com.salesforce.cte.helper.TestAdvisorPortalException;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Connector class providing support for REST calls from client to portal.
 * 
 * @author gneumann
 * @author Yibing Tao
 */
public class Connector {
	private static final Logger LOGGER = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
	
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String DEVICE_REQUEST = "/services/oauth2/token?response_type=device_code&scope=refresh_token%20api";
	private static final String ENCODING = "UTF8";
	private static final String GRANTTYPE_DEVICE = "/services/oauth2/token?grant_type=device";
	private static final Header PRETTY_PRINT_HEADER = new BasicHeader("X-PrettyPrint", "1");
	private static final String REFRESH_REQUEST = "/services/oauth2/token?grant_type=refresh_token";

	private static final String CLIENT_ID_PROPERTY = "portal.clientid";
	private static final String AUTHURL_PROPERTY = "auth.url";
	private static final String BASE_URL_PROPERTY = "portal.url";

	private HttpClientBuilder builder;
	private Registry registry; //registry properties contain all settings
	private SecretsManager secretsManager; //secrets manager do encrypt/decrypt
	// TODO replace getDefaultHostnameVerifier() with a more specific verifier
	private SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(SSLContexts.createDefault(),
				new String[] { "TLSv1.2" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());;
	private String baseURL;
	private String authURL;
	private String clientId;

	public Connector(Registry registry, SecretsManager secretsManager) throws IOException, TestAdvisorCipherException {
		this.registry = registry;
		this.secretsManager = secretsManager;

		//load properties
		baseURL = this.registry.getRegistryProperties().getProperty(BASE_URL_PROPERTY, "");
		authURL=this.registry.getRegistryProperties().getProperty(AUTHURL_PROPERTY, "");

		clientId = this.registry.getRegistryProperties().getProperty(CLIENT_ID_PROPERTY, "");
		if (clientId.isEmpty()){
			throw new IllegalArgumentException("Client ID not available; Please add Client Id property!");
		}
		clientId = URLEncoder.encode(clientId, ENCODING);	
	}

	/**
	 * Authorizes this system (aka "device") with the Portal app using
	 * <a href="https://help.salesforce.com/articleView?id=sf.remoteaccess_oauth_device_flow.htm&type=5">
	 * OAuth 2.0 Device Flow</a>.
	 * @throws IOException
	 * This exception is thrown when it failed to access registry properties
	 * @throws TestAdvisorPortalException
	 * throw in case of problems during authorization and response conversion; any HTTP status code other than
	 * {@link HttpStatus#SC_OK} will also trigger an exception
	 * @throws InterruptedException
	 * This exception is thrown when current thread failed to sleep
	 * @throws TestAdvisorCipherException
	 * throw in case of any cipher related failure
	 */
	public void  setupConnectionWithPortal() throws IOException, TestAdvisorPortalException, InterruptedException, 
										 TestAdvisorCipherException {
		//Send Device Request
		String deviceRequestAuthorizationURL = authURL + DEVICE_REQUEST + "&client_id=" + clientId;
		try (CloseableHttpClient httpClient = getHttpClient()){

			HttpPost httpPost = new HttpPost(deviceRequestAuthorizationURL);
			String result;
			try (CloseableHttpResponse response = httpClient.execute(httpPost)){
				// verify response is HTTP OK
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					throw new TestAdvisorPortalException("Optaining device code has failed!");
				}
				result = EntityUtils.toString(response.getEntity());
			}

			JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
			String userCode = jsonObject.getString("user_code");
			String deviceCode = jsonObject.getString("device_code");
			String verificationURI = jsonObject.getString("verification_uri");
			int pollingWaitTime = jsonObject.getInt("interval");

			System.out.println("\nFor the authorization to succeed, please log out of any Salesforce org!\n");
			System.out.println("Now open the following URL in your browser:");
			System.out.println(verificationURI + "?user_code=" + userCode);
			System.out.println(
					"To authenticate yourself, log in into TestAdvisor Portal using the API credentials provided to you.");
			System.out.println("If the authentication was successful, you will see the message \"You're Connected\".");
			System.out.println("When connection is confirmed, press ENTER to continue...");
			new Scanner(System.in).nextLine();
			System.out.println("Waiting for device code confirmation!");

			// give a few secs to perform the connect confirmation before polling
			Thread.sleep(pollingWaitTime);
			
			/*
				* Obtain Device Code
				*/
			String encodedDeviceCode = URLEncoder.encode(deviceCode, ENCODING);
			String deviceCodeURL = authURL + GRANTTYPE_DEVICE + "&client_id=" + clientId + "&code="
					+ encodedDeviceCode;

			httpPost = new HttpPost(deviceCodeURL);
			try (CloseableHttpResponse response = httpClient.execute(httpPost)){
				// verify response is HTTP OK
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK)
					throw new TestAdvisorPortalException("Device code was not confirmed!");
				System.out.println("Device code has been confirmed!");

				result = EntityUtils.toString(response.getEntity());
			}
			jsonObject = (JSONObject) new JSONTokener(result).nextValue();

			secretsManager.setAccessToken(jsonObject.getString("access_token"));
			secretsManager.setRefreshToken(jsonObject.getString("refresh_token"));
			registry.saveRegistryProperty(BASE_URL_PROPERTY, jsonObject.getString("instance_url"));

			System.out.println("Authorization successful!");
		}//close http client
	}

	/**
	 * Refreshes the access token issued for this system.
	 * @throws IOException
	 * throws when fail to contact portal
	 * @throws TestAdvisorPortalException
	 * throws in case of problems during authorization and response conversion; any HTTP status code other than
	 * {@link HttpStatus#SC_OK} will also trigger an exception
	 * @throws TestAdvisorCipherException
	 * throws in case of any cipher related failure
	 */
	public void connectToPortal() throws IOException, TestAdvisorPortalException, TestAdvisorCipherException {
		String refreshTokenFromCredentials = secretsManager.getRefreshToken();
		if (refreshTokenFromCredentials.length() == 0)
			throw new IllegalArgumentException("Unable to retrieve refresh token");

		/*
		 * Send Refresh Token Request
		 */
		LOGGER.log(Level.INFO, "Start refresh token");
		String deviceRequestAuthorizationURL = authURL + REFRESH_REQUEST + "&client_id=" + clientId
				+ "&refresh_token=" + refreshTokenFromCredentials;

		try(CloseableHttpClient httpClient = getHttpClient()){
			HttpPost httpPost = new HttpPost(deviceRequestAuthorizationURL);
			try (CloseableHttpResponse response = httpClient.execute(httpPost)){
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					throw new TestAdvisorPortalException("Obtaining new access token has failed!");
				}
				String result = EntityUtils.toString(response.getEntity());
				JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
				secretsManager.setAccessToken(jsonObject.getString("access_token"));
			}
		}
	}

	/**
	 * Post to an apex endpoint to send the payload and get the response
	 * 
	 * @param endpoint	
	 * The Apex upload endpoint 
	 * @param payload	
	 * The pay load to upload
	 * @return 
	 * String response from the Apex endpoint
	 * @throws TestAdvisorPortalException
	 * throws when portal rejects the payload
	 * @throws IOException
	 * throws when fail to contact portal
	 * @throws TestAdvisorCipherException
	 * throws for any cipher related failure
	 */
	public String postApex(String endpoint, String payload) 
			throws IOException, TestAdvisorPortalException, TestAdvisorCipherException {
		String fullUrl = (endpoint.startsWith("/")) ? this.baseURL + endpoint : this.baseURL + "/" + endpoint;
		
		try (CloseableHttpClient httpClient = getHttpClient()){
			HttpPost httpPost = new HttpPost(fullUrl);
			httpPost.addHeader(getOAuthHeader());
			httpPost.addHeader(PRETTY_PRINT_HEADER);
			// The message we are going to post
			StringEntity body = new StringEntity(payload);
			body.setContentType(CONTENT_TYPE_JSON);
			httpPost.setEntity(body);

			// Execute the Post request
			try (CloseableHttpResponse response = httpClient.execute(httpPost)){
				// Process the result
				int statusCode = response.getStatusLine().getStatusCode();
				String responseString = EntityUtils.toString(response.getEntity());
				LOGGER.log(Level.INFO,responseString);

				if (statusCode != HttpStatus.SC_CREATED) {
					throw new TestAdvisorPortalException(
						String.format("Post unsuccessfull, Status:%d%nMessage%n%s%n", statusCode,responseString));
				}
				return responseString;
			}
		}	
	 }

	/**
	 * set a new http client builder
	 * @param builder
	 * http client builder should use
	 */
	public void setBuilder(HttpClientBuilder builder){
		this.builder = builder;
	}

	/**
	 * Creates a closable HTTP client, using SSL Connection Socket factory with the
	 * highest available TLS version supported by the JDK used as runtime
	 * environment.
	 * 
	 * To use TLS 1.3 it is required to use JDK 11 or newer!
	 */
	private CloseableHttpClient getHttpClient() {
		return builder == null ? HttpClients.custom().setSSLSocketFactory(connectionSocketFactory).useSystemProperties().build()
				: this.builder.build();
	}

	/**
	 * 
	 * @return
	 * HTTP auth header
	 * @throws TestAdvisorPortalException
	 */
	private Header getOAuthHeader() throws TestAdvisorCipherException{
		return new BasicHeader("Authorization", "OAuth " + secretsManager.getAccessToken());
	}
}
