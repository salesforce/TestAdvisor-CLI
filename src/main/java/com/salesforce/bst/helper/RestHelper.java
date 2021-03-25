/**
 * 
 */
package com.salesforce.bst.helper;

import java.net.URLEncoder;

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
	private static final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private static final String LOGINURL = "https://test.salesforce.com";
	private static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";

	private static SSLConnectionSocketFactory connectionSocketFactory = null;
	private static String baseUri;
	private static Header oauthHeader;
	
	/**
	 * Login into BST Portal app using OAuth and Salesforce as ID Provider.
	 * 
	 * @param username a valid Salesforce user name
	 * @param password password for this user
	 * @param clientId ID of the connected app in the Salesforce org
	 * @param clientSecret this connected app's secret
	 * @throws Exception in case of problems during login and response conversion; any HTTP status
	 * code other than {@link HttpStatus#SC_OK} will also trigger an exception
	 */
	public static void login(final String username, final String password, final String clientId, final String clientSecret) throws Exception {
		final String encoding = "UTF8";
		String encodedUsername = URLEncoder.encode(username, encoding);
		String encodedPassword = URLEncoder.encode(password, encoding);
		String encodedClientId = URLEncoder.encode(clientId, encoding);
		String encodedClientSecret = URLEncoder.encode(clientSecret, encoding);
		String loginURL = LOGINURL + GRANTSERVICE + "&client_id=" + encodedClientId + "&client_secret=" + encodedClientSecret
				+ "&username=" + encodedUsername + "&password=" + encodedPassword;		
		CloseableHttpClient httpClient = getHttpClient();

		// Login requests must be POSTs
		HttpPost httpPost = new HttpPost(loginURL);
		HttpResponse response = httpClient.execute(httpPost);

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			throw new Exception("Login has failed!");
		}
		String result = EntityUtils.toString(response.getEntity());

		JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
		String loginAccessToken = jsonObject.getString("access_token");
		String loginInstanceUrl = jsonObject.getString("instance_url");

		baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION;
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);
		System.out.println("Login successful!");
		System.out.println("Base URI: " + baseUri);
	}

	/**
	 * Create (i.e. HTTP Post) a record at the given URI using the provided information.
	 * 
	 * @param uri the URI relative to base URI, e.g. "/sobjects/Account/"
	 * @param object the JSONObject to get posted
	 * @return response converted into a JSONObject for easy retrieval of information
	 * @throws Exception in case of problems during posting and response conversion; any HTTP status
	 * code other than {@link HttpStatus#SC_CREATED} will also trigger an exception
	 */
	public static JSONObject create(String uri, JSONObject object) throws Exception {
		if (Strings.isNullOrEmpty(baseUri) || oauthHeader == null) {
			throw new IllegalStateException("Cannot post information because prior call to login() is missing or had failed");
		}
		// ensure there is a "/" between baseUri and uri
		String fullUri = (uri.startsWith("/")) ? baseUri + uri : baseUri + "/" + uri;

		CloseableHttpClient httpClient = getHttpClient();
		HttpPost httpPost = new HttpPost(fullUri);
		httpPost.addHeader(oauthHeader);
		httpPost.addHeader(prettyPrintHeader);
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
	 * @param uri the URI relative to base URI, e.g. "/query?q=Select+Id,+Name+From+Account+Limit+5"
	 * @return response converted into a JSONObject for easy retrieval of information
	 * @throws Exception in case of problems during querying and response conversion; any HTTP status
	 * code other than {@link HttpStatus#SC_OK} will also trigger an exception
	 */
	public static JSONObject query(String uri) throws Exception {
		if (Strings.isNullOrEmpty(baseUri) || oauthHeader == null) {
			throw new IllegalStateException("Cannot post information because prior call to login() is missing or had failed");
		}
		// ensure there is a "/" between baseUri and uri
		String fullUri = (uri.startsWith("/")) ? baseUri + uri : baseUri + "/" + uri;

		CloseableHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(fullUri);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);

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
	 * @param uri the URI relative to base URI, e.g. "/sobjects/Account/" + accountId
	 * @param object the JSONObject to get posted
	 * @throws Exception in case of problems during updating; any HTTP status
	 * code other than {@link HttpStatus#SC_NO_CONTENT} will also trigger an exception
	 */
	public static void update(String uri, JSONObject object) throws Exception {
		if (Strings.isNullOrEmpty(baseUri) || oauthHeader == null) {
			throw new IllegalStateException("Cannot post information because prior call to login() is missing or had failed");
		}
		// ensure there is a "/" between baseUri and uri
		String fullUri = (uri.startsWith("/")) ? baseUri + uri : baseUri + "/" + uri;

		CloseableHttpClient httpClient = getHttpClient();
		HttpPatch httpPatch = new HttpPatch(fullUri);
		httpPatch.addHeader(oauthHeader);
		httpPatch.addHeader(prettyPrintHeader);
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
	 * @param uri the URI relative to base URI, e.g. "/sobjects/Account/" + accountId
	 * @throws Exception in case of problems during deletion; any HTTP status
	 * code other than {@link HttpStatus#SC_NO_CONTENT} will also trigger an exception
	 */
	public static void delete(String uri) throws Exception {
		if (Strings.isNullOrEmpty(baseUri) || oauthHeader == null) {
			throw new IllegalStateException("Cannot post information because prior call to login() is missing or had failed");
		}
		// ensure there is a "/" between baseUri and uri
		String fullUri = (uri.startsWith("/")) ? baseUri + uri : baseUri + "/" + uri;

		CloseableHttpClient httpClient = getHttpClient();
		HttpDelete httpDelete = new HttpDelete(fullUri);
		httpDelete.addHeader(oauthHeader);
		httpDelete.addHeader(prettyPrintHeader);

		// Execute the DELETE request
		HttpResponse response = httpClient.execute(httpDelete);

		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_NO_CONTENT) {
			throw new Exception("Deletion unsuccessful. Status code returned is " + statusCode);
		}
	}

	/**
	 * Closes the HTTP client connection.
	 * 
	 * @throws Exception in case of problems during closing of the connection
	 */
	public static void close() throws Exception {
		if (Strings.isNullOrEmpty(baseUri) || oauthHeader == null) {
			throw new IllegalStateException("Cannot post information because prior call to login() is missing or had failed");
		}
		new HttpPost(baseUri).releaseConnection();
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
}
