/**
 * 
 */
package com.salesforce.bst.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Scanner;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Strings;

/**
 * Helper class providing support for REST calls from client to portal.
 * 
 * @author gneumann
 */
public class RestHelper {
	private static final String REST_ENDPOINT = "/services/data";
	private static final String API_VERSION = "/v50.0";
	private static final Header PRETTY_PRINT_HEADER = new BasicHeader("X-PrettyPrint", "1");
	private static final String ENCODING = "UTF8";

	private static final String REST_CONFIG_FILE = "rest_config.properties";
	private static final String CLIENT_ID_PROPERTY = "portal.clientid";
	private static final String AUTHURL_PROPERTY = "auth.url";
	private static final String BASE_URL_PROPERTY = "portal.url";

	private static SSLConnectionSocketFactory connectionSocketFactory = null;
	private static Header oauthHeader;
	private static Properties restConfig = null;

	public static void main(String[] args) {
		try {
			SecretsHelper.setPassPhrase("Hello World!", false);

//			SecretsHelper.clearAccessToken();

//			RestHelper.revoke();
			// give 10 secs between revocation and authorization
//			try { Thread.sleep(2000L); } catch (InterruptedException ie) { ; }
//			if (true) return;
//			RestHelper.authorize();
//			if (true) return;

			// query for up to 5 test suites
			JSONObject listOfTestSuites = RestHelper.query("/query?q=Select+Id,+Name+From+Test_Suite__c+Limit+5");
			System.out.println("Test Suite records found: ");
			JSONArray j = listOfTestSuites.getJSONArray("records");
			String testSuiteId = null;
			for (int i = 0; i < j.length(); i++) {
				String testSuiteName = listOfTestSuites.getJSONArray("records").getJSONObject(i).getString("Name");
				String tempSuiteId = listOfTestSuites.getJSONArray("records").getJSONObject(i).getString("Id");
				if (testSuiteName.startsWith("DrillBit"))
					testSuiteId = tempSuiteId;
				System.out.println(tempSuiteId + " " + testSuiteName);
			}

			// create the JSON object containing the new test suite details.
			JSONObject testSuite = new JSONObject();
			testSuite.put("Name", "DrillBit Test " + System.currentTimeMillis());

			// create new test suite
			JSONObject newTestSuiteResponse = RestHelper.create("/sobjects/Test_Suite__c/", testSuite);
			String newTestSuiteId = newTestSuiteResponse.getString("id");
			System.out.println("New test suite's ID: " + newTestSuiteId);
			
			// update new account
			testSuite.put("Name", "DrillBit Test UPDATED at " + System.currentTimeMillis());
			RestHelper.update("/sobjects/Test_Suite__c/" + newTestSuiteId, testSuite);
			System.out.println("Updated account with ID: " + newTestSuiteId);

			// delete old test suite or new one if no old could be found
			String testSuiteToBeDeleted = Strings.isNullOrEmpty(testSuiteId) ? newTestSuiteId : testSuiteId;
			RestHelper.delete("/sobjects/Test_Suite__c/" + testSuiteToBeDeleted);
			System.out.println("Deleted test suite with ID: " + testSuiteToBeDeleted);
			
			// close connection
			RestHelper.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Authorizes this system (aka "device") with the Portal app using OAuth 2.0 Device Flow.
	 * 
	 * @throws Exception in case of problems during authorization and response conversion; any HTTP status
	 * code other than {@link HttpStatus#SC_OK} will also trigger an exception
	 */
	@SuppressWarnings("resource")
	public static void authorize() throws Exception {
		try {
			if (SecretsHelper.getAccessToken().length() > 0)
				return;
		} catch (Exception e) { ; /* ignore because access token is not yet set */}
		
		/*
		 * Send Device Request
		 */
		final String DEVICE_REQUEST = "/services/oauth2/token?response_type=device_code&scope=refresh_token%20api";
		String deviceRequestAuthorizationURL = getAuthenticationURL() + DEVICE_REQUEST + "&client_id=" + getClientId();		
		CloseableHttpClient httpClient = getHttpClient();

		HttpPost httpPost = new HttpPost(deviceRequestAuthorizationURL);
		HttpResponse response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			throw new Exception("Authorization has failed!");
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
		System.out.println("To authenticate yourself, log in into DrillBit Portal using the API credentials provided to you.");
		System.out.println("If the authentication was successful, you will see the message \"You're Connected\".");
	    System.out.println("When connection is confirmed, press ENTER to continue...");
	    new Scanner(System.in).nextLine(); 
		System.out.println("Waiting for device code confirmation!");

		// give a few secs to perform the connect confirmation before polling
		try { Thread.sleep(pollingWaitTime); } catch (InterruptedException ie) { ; }
		
		/*
		 * Obtain Device Code
		 */
		final String GRANTTYPE_DEVICE = "/services/oauth2/token?grant_type=device";
		String encodedDeviceCode = URLEncoder.encode(deviceCode, ENCODING);
		String deviceCodeURL = getAuthenticationURL() + GRANTTYPE_DEVICE + "&client_id=" + getClientId() + "&code=" + encodedDeviceCode;		

		httpPost = new HttpPost(deviceCodeURL);
		response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK)
			throw new Exception("Device code was not confirmed!");
		System.out.println("Device code has been confirmed!");

		result = EntityUtils.toString(response.getEntity());
		jsonObject = (JSONObject) new JSONTokener(result).nextValue();
		String accessToken = jsonObject.getString("access_token");
		String instanceUrl = jsonObject.getString("instance_url");
		String refreshToken = jsonObject.getString("refresh_token");
		
		SecretsHelper.setAccessToken(accessToken);
		SecretsHelper.setRefreshToken(refreshToken);
		setBaseURL(instanceUrl + REST_ENDPOINT + API_VERSION);

		System.out.println("Authorization successful!");
	}

	/**
	 * Create (i.e. HTTP Post) a record at the given URI using the provided information.
	 * 
	 * @param url the URL relative to base URI, e.g. "/sobjects/Account/"
	 * @param object the JSONObject to get posted
	 * @return response converted into a JSONObject for easy retrieval of information
	 * @throws Exception in case of problems during posting and response conversion; any HTTP status
	 * code other than {@link HttpStatus#SC_CREATED} will also trigger an exception
	 */
	public static JSONObject create(String url, JSONObject object) throws Exception {
		// ensure there is a "/" between baseURL and URL
		String fullUrl = (url.startsWith("/")) ? getBaseURL() + url : getBaseURL() + "/" + url;

		CloseableHttpClient httpClient = getHttpClient();
		HttpPost httpPost = new HttpPost(fullUrl);
		httpPost.addHeader(getOAuthHeader());
		httpPost.addHeader(PRETTY_PRINT_HEADER);
		// The message we are going to post
		StringEntity body = new StringEntity(object.toString(1));
		body.setContentType("application/json");
		httpPost.setEntity(body);

		// Execute the POST request
		HttpResponse response = httpClient.execute(httpPost);

		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		String responseString = null;
		if (statusCode == HttpStatus.SC_CREATED) {
			responseString = EntityUtils.toString(response.getEntity());
		} else {
			throw new Exception("Insertion unsuccessful. Status code returned is " + statusCode);
		}
		return new JSONObject(responseString);
	}

	/**
	 * Query (i.e. HTTP Get) for records using the given URI.
	 * 
	 * @param url the URI relative to base URI, e.g. "/query?q=Select+Id,+Name+From+Account+Limit+5"
	 * @return response converted into a JSONObject for easy retrieval of information
	 * @throws Exception in case of problems during querying and response conversion; any HTTP status
	 * code other than {@link HttpStatus#SC_OK} will also trigger an exception
	 */
	public static JSONObject query(String url) throws Exception {
		// ensure there is a "/" between baseURL and URL
		String fullUrl = (url.startsWith("/")) ? getBaseURL() + url : getBaseURL() + "/" + url;

		CloseableHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(fullUrl);
		httpGet.addHeader(getOAuthHeader());
		httpGet.addHeader(PRETTY_PRINT_HEADER);

		// Execute the GET request
		HttpResponse response = httpClient.execute(httpGet);
		
		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		String responseString = null;
		if (statusCode == HttpStatus.SC_OK) {
			responseString = EntityUtils.toString(response.getEntity());
		} else {
			throw new Exception("Query unsuccessful. Status code returned is " + statusCode);
		}
		return new JSONObject(responseString);
	}

	/**
	 * Updates (i.e. HTTP Patch) a record at the given URI using the provided information.
	 * 
	 * @param url the URI relative to base URI, e.g. "/sobjects/Account/" + accountId
	 * @param object the JSONObject to get posted
	 * @throws Exception in case of problems during updating; any HTTP status
	 * code other than {@link HttpStatus#SC_NO_CONTENT} will also trigger an exception
	 */
	public static void update(String url, JSONObject object) throws Exception {
		// ensure there is a "/" between baseURL and URL
		String fullUrl = (url.startsWith("/")) ? getBaseURL() + url : getBaseURL() + "/" + url;

		CloseableHttpClient httpClient = getHttpClient();
		HttpPatch httpPatch = new HttpPatch(fullUrl);
		httpPatch.addHeader(getOAuthHeader());
		httpPatch.addHeader(PRETTY_PRINT_HEADER);
		// The message we are going to post
		StringEntity body = new StringEntity(object.toString(1));
		body.setContentType("application/json");
		httpPatch.setEntity(body);

		// Execute the PATCH request
		HttpResponse response = httpClient.execute(httpPatch);

		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_NO_CONTENT) {
			throw new Exception("Update unsuccessful. Status code returned is " + statusCode);
		}
	}

	/**
	 * Deletes (i.e. HTTP Delete) a record at the given URI.
	 * 
	 * @param url the URI relative to base URI, e.g. "/sobjects/Account/" + accountId
	 * @throws Exception in case of problems during deletion; any HTTP status
	 * code other than {@link HttpStatus#SC_NO_CONTENT} will also trigger an exception
	 */
	public static void delete(String url) throws Exception {
		// ensure there is a "/" between baseURL and URL
		String fullUrl = (url.startsWith("/")) ? getBaseURL() + url : getBaseURL() + "/" + url;

		CloseableHttpClient httpClient = getHttpClient();
		HttpDelete httpDelete = new HttpDelete(fullUrl);
		httpDelete.addHeader(getOAuthHeader());
		httpDelete.addHeader(PRETTY_PRINT_HEADER);

		// Execute the DELETE request
		HttpResponse response = httpClient.execute(httpDelete);

		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_NO_CONTENT) {
			throw new Exception("Deletion of record " + url + " unsuccessful. Status code returned is " + statusCode);
		}
	}

	/**
	 * Closes the HTTP client connection.
	 * 
	 * @throws Exception in case of problems during closing of the connection
	 */
	public static void close() throws Exception {
		new HttpPost(getBaseURL()).releaseConnection();
	}

	/**
	 * Revokes API access for access token currently on file.
	 * @throws Exception if accessing the token or revocation of API access has failed
	 */
	private static void revokeDefunct() throws Exception {
		String accessToken = SecretsHelper.getAccessToken();
		if (accessToken.length() == 0)
			return;

		String revocationURL = getAuthenticationURL() + "/services/oauth2/revoke?token=" + URLEncoder.encode(accessToken, ENCODING);		
		CloseableHttpClient httpClient = getHttpClient();

		HttpPost httpPost = new HttpPost(revocationURL);
		HttpResponse response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			// TODO always getting status 302 :-(
			throw new Exception("Revoking authorization has failed! HTTPS Status: " + statusCode);
		}
		SecretsHelper.clearAccessToken();
		System.out.println("API access revoken for user account");
	}

	/**
	 * Creates a closable HTTP client, using SSL Connection Socket factory with the
	 * highest available TLS version supported by the JDK used as runtime environment.
	 * 
	 * To use TLS 1.3 it is required to use JDK 11 or newer!
	 */
	private static CloseableHttpClient getHttpClient() {
		if (connectionSocketFactory == null) {
			connectionSocketFactory = new SSLConnectionSocketFactory(SSLContexts.createDefault(),
					(isJDK11orNewer()) ? new String[] { "TLSv1.3" } : new String[] { "TLSv1.2" }, null,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier()); // TODO look into more specific verifiers
		}
		return HttpClients.custom().setSSLSocketFactory(connectionSocketFactory).build();
	}

	/**
	 * To use TLS 1.3 it is required to use JDK 11 or newer!
	 * @return true if JDK is 11 or newer, otherwise false
	 */
	private static boolean isJDK11orNewer() {
		String version = System.getProperty("java.version");
		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}
		int versionInt = Integer.parseInt(version);
		if (versionInt <= 10) {
			System.err.println("---------------------------------------------------------");
			System.err.println("For hightend security please upgrade JDK to v11 or newer!");
			System.err.println("---------------------------------------------------------");
			return false;
		}
		return true;
	}

	/**
	 * Gets the client id from REST config file.
	 * 
	 * @return client ID encoded for use in URL or empty string if none could be
	 * found in REST config file
	 * @throws Exception if accessing REST config file failed for any reason
	 */
	private static String getClientId() throws Exception {
		loadRestConfig();
		String value = restConfig.getProperty(CLIENT_ID_PROPERTY, "");
		return (value.length() == 0) ? value : URLEncoder.encode(value, ENCODING);
	}

	/**
	 * Gets the OAuth header.
	 * 
	 * @return OAuth header
	 * @throws Exception 
	 */
	private static Header getOAuthHeader() throws Exception {
		if (oauthHeader == null)
			oauthHeader = new BasicHeader("Authorization", "OAuth " + SecretsHelper.getAccessToken());
		return oauthHeader;
	}

	/**
	 * Gets the authentication URL from REST config file.
	 * 
	 * @return authentication URL or empty string if none could be found in REST config file
	 * @throws Exception if accessing REST config file failed for any reason
	 */
	private static String getAuthenticationURL() throws Exception {
		loadRestConfig();
		return restConfig.getProperty(AUTHURL_PROPERTY, "");
	}

	/**
	 * Saves the given base URL to REST config file.
	 * 
	 * @param baseURL base URL of Portal app
	 * @throws Exception if accessing REST config file failed for any reason
	 */
	private static void setBaseURL(String baseURL) throws Exception {
		if (Strings.isNullOrEmpty(baseURL))
			throw new IllegalArgumentException("Base URL must not be null or empty!");
		loadRestConfig();
		restConfig.setProperty(BASE_URL_PROPERTY, baseURL);
		saveRestConfig();
	}

	/**
	 * Gets the base URL from REST config file.
	 * 
	 * @return base URL or empty string if none could be found in REST config file
	 * @throws Exception if accessing REST config file failed for any reason
	 */
	private static String getBaseURL() throws Exception {
		loadRestConfig();
		String baseURL = restConfig.getProperty(BASE_URL_PROPERTY, "");
		if (baseURL.length() == 0)
			throw new IllegalArgumentException("Base URL not available; authentication is required!");
		return baseURL;
	}

	private static void loadRestConfig() throws Exception {
		if (restConfig != null)
			return;

		restConfig = new Properties();
		File credentialsFile = new File(REST_CONFIG_FILE);
		if (credentialsFile.canRead()) {
			// file exists
			InputStream input = new FileInputStream(credentialsFile);
			restConfig.load(input);
		}
	}

	private static void saveRestConfig() throws Exception {
		OutputStream output = new FileOutputStream(REST_CONFIG_FILE);
		// save properties to project root folder
		restConfig.store(output, null);
	}
}
