package com.salesforce.cqe.drillbit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Strings;
import com.salesforce.cqe.helper.DrillbitException;
import com.salesforce.cqe.helper.SecretsManager;

/**
 * Connector class providing support for REST calls from client to portal.
 * 
 * @author gneumann
 * @author Yibing Tao
 */
public class Connector implements AutoCloseable {
	private static final String API_VERSION = "/v50.0";
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String DEVICE_REQUEST = "/services/oauth2/token?response_type=device_code&scope=refresh_token%20api";
	private static final String ENCODING = "UTF8";
	private static final String GRANTTYPE_DEVICE = "/services/oauth2/token?grant_type=device";
	private static final Header PRETTY_PRINT_HEADER = new BasicHeader("X-PrettyPrint", "1");
	private static final String REFRESH_REQUEST = "/services/oauth2/token?grant_type=refresh_token";
	private static final String REST_ENDPOINT = "/services/data";

	private static final String REST_CONFIG_FILE = "rest_config.properties";
	private static final String CLIENT_ID_PROPERTY = "portal.clientid";
	private static final String AUTHURL_PROPERTY = "auth.url";
	private static final String BASE_URL_PROPERTY = "portal.url";

	private SecretsManager secretsManager;
	private SSLConnectionSocketFactory connectionSocketFactory = null;
	private Header oauthHeader;
	private Properties restConfig = new Properties();
	private String baseURL;
	private String authURL;
	private String clientId;
	private CloseableHttpClient httpClient;
	private HttpPost httpPost;

	public Connector(SecretsManager secretsManager) 
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
		this.secretsManager = secretsManager;
		//load configuration file
		File credentialsFile = new File(REST_CONFIG_FILE);
		if (!credentialsFile.canRead()) {
			throw new IllegalArgumentException("Failed to load configuration file:"+REST_CONFIG_FILE);
		}
		try(InputStream input = new FileInputStream(credentialsFile)){
			restConfig.load(input);
		}

		//load properties
		baseURL = restConfig.getProperty(BASE_URL_PROPERTY, "");
		if (baseURL.length() == 0)
			throw new IllegalArgumentException("Base URL not available; authentication is required!");
		authURL=restConfig.getProperty(AUTHURL_PROPERTY, "");

		clientId = restConfig.getProperty(CLIENT_ID_PROPERTY, "");
		clientId = (clientId.length() == 0) ? clientId : URLEncoder.encode(clientId, ENCODING);

		oauthHeader = new BasicHeader("Authorization", "OAuth " + secretsManager.getAccessToken());
	}

	public Connector(CloseableHttpClient httpClient,HttpPost httpPost,SecretsManager secretsManager) 
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException{
		//constructor for unit test
		this(secretsManager);
		this.httpClient=httpClient;
		this.httpPost=httpPost;
		
	}

	/**
	 * Authorizes this system (aka "device") with the Portal app using
	 * <a href="https://help.salesforce.com/articleView?id=sf.remoteaccess_oauth_device_flow.htm&type=5">
	 * OAuth 2.0 Device Flow</a>.
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws DrillbitException
	 * @throws InterruptedException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * 
	 * @throws Exception in case of problems during authorization and response
	 *                   conversion; any HTTP status code other than
	 *                   {@link HttpStatus#SC_OK} will also trigger an exception
	 */
	public void setupConnectionWithPortal() throws ClientProtocolException, IOException, DrillbitException, 
					InterruptedException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		try {
			if (secretsManager.getAccessToken().length() > 0)
				return;
		} catch (Exception e) {	; /* ignore because access token is not yet set */}

		/*
		 * Send Device Request
		 */
		String deviceRequestAuthorizationURL = authURL + DEVICE_REQUEST + "&client_id=" + clientId;
		httpClient = getHttpClient();

		httpPost = new HttpPost(deviceRequestAuthorizationURL);
		HttpResponse response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			throw new DrillbitException("Optaining device code has failed!");
		}
		String result = EntityUtils.toString(response.getEntity());

		JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
		String userCode = jsonObject.getString("user_code");
		String deviceCode = jsonObject.getString("device_code");
		String verificationURI = jsonObject.getString("verification_uri");
		int pollingWaitTime = jsonObject.getInt("interval");

		System.out.println("\nFor the authorization to succeed, please log out of any Salesforce org!\n");
		System.out.println("Now open the following URL in your browser:");
		System.out.println(verificationURI + "?user_code=" + userCode);
		System.out.println(
				"To authenticate yourself, log in into DrillBit Portal using the API credentials provided to you.");
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
		response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK)
			throw new DrillbitException("Device code was not confirmed!");
		System.out.println("Device code has been confirmed!");

		result = EntityUtils.toString(response.getEntity());
		jsonObject = (JSONObject) new JSONTokener(result).nextValue();
		String accessToken = jsonObject.getString("access_token");
		String instanceUrl = jsonObject.getString("instance_url");
		String refreshToken = jsonObject.getString("refresh_token");

		secretsManager.setAccessToken(accessToken);
		secretsManager.setRefreshToken(refreshToken);
		setBaseURL(instanceUrl + REST_ENDPOINT + API_VERSION);

		System.out.println("Authorization successful!");
	}

	/**
	 * Refreshes the access token issued for this system.
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws DrillbitException
	 * 
	 * @throws Exception in case of problems during authorization and response
	 *                   conversion; any HTTP status code other than
	 *                   {@link HttpStatus#SC_OK} will also trigger an exception
	 */
	public void connectToPortal() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, 
				IOException, DrillbitException {
		String refreshTokenFromCredentials = secretsManager.getRefreshToken();
		if (refreshTokenFromCredentials.length() == 0)
			throw new IllegalArgumentException("Unable to retrieve refresh token");

		/*
		 * Send Refresh Token Request
		 */
		String deviceRequestAuthorizationURL = authURL + REFRESH_REQUEST + "&client_id=" + clientId
				+ "&refresh_token=" + refreshTokenFromCredentials;

		httpClient = getHttpClient();
		httpPost = new HttpPost(deviceRequestAuthorizationURL);
		HttpResponse response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			throw new DrillbitException("Obtaining new access token has failed!");
		}
		String result = EntityUtils.toString(response.getEntity());
		JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
		secretsManager.setAccessToken(jsonObject.getString("access_token"));
	}


	/**
	 * Post to an apex endpoint to send the payload and get the response
	 * 
	 * @param endpoint	The Apex endpoint 
	 * @param payload	The pay load to upload
	 * @return JSON object response from the Apex endpoint
	 * @throws DrillbitException
	 * @throws IOException
	 * @throws JSONException
	 * @throws ParseException
	 */
	public JSONObject postApex(String endpoint, String payload) 
			throws ParseException, JSONException, IOException, DrillbitException {
		String fullUrl = (endpoint.startsWith("/")) ? this.baseURL + endpoint : this.baseURL + "/" + endpoint;
		
		httpClient = httpClient==null ? getHttpClient() : httpClient;
		httpPost = httpPost==null ? new HttpPost(fullUrl) : httpPost;
		httpPost.addHeader(oauthHeader);
		httpPost.addHeader(PRETTY_PRINT_HEADER);
		// The message we are going to post
		StringEntity body = new StringEntity(payload);
		body.setContentType(CONTENT_TYPE_JSON);
		httpPost.setEntity(body);

		// Execute the Post request
		HttpResponse response = httpClient.execute(httpPost);

		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		
		if (statusCode != HttpStatus.SC_CREATED) {
			throw new DrillbitException(
				String.format("Post unsuccessfull, Status:%d%n", statusCode));
		}
		String responseString = EntityUtils.toString(response.getEntity());
		return new JSONObject(responseString);		
	 }

	/**
	 * Creates a closable HTTP client, using SSL Connection Socket factory with the
	 * highest available TLS version supported by the JDK used as runtime
	 * environment.
	 * 
	 * To use TLS 1.3 it is required to use JDK 11 or newer!
	 */
	private CloseableHttpClient getHttpClient() {
		if (connectionSocketFactory == null) {
			// TODO replace getDefaultHostnameVerifier() with a more specific verifier
			connectionSocketFactory = new SSLConnectionSocketFactory(SSLContexts.createDefault(),
					new String[] { "TLSv1.2" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier()); 
		}
		return HttpClients.custom().setSSLSocketFactory(connectionSocketFactory).build();
	}

	/**
	 * Saves the given base URL to REST config file.
	 * 
	 * @param baseURL base URL of Portal app
	 * @throws IOException if accessing REST config file failed for any reason
	 */
	private void setBaseURL(String baseURL) throws IOException  {
		if (Strings.isNullOrEmpty(baseURL))
			throw new IllegalArgumentException("Base URL must not be null or empty!");
		restConfig.setProperty(BASE_URL_PROPERTY, baseURL);
		saveRestConfig();
	}

	private void saveRestConfig() throws IOException {
		try(OutputStream output = new FileOutputStream(REST_CONFIG_FILE)){
			// save properties to project root folder
			restConfig.store(output, null);
		}
	}

	@Override
	public void close() throws Exception {
		httpClient.close();
	}
}
