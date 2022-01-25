/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.google.common.base.Strings;
import com.salesforce.cte.helper.TestAdvisorCipherException;

/**
 * This class provides support for handling pass phrase and access token required
 * to connect the client to the portal.
 * 
 * @author gneumann
 * @author Yibing Tao
 */
public class SecretsManager {
	private static final Logger LOGGER = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
	
	private static final String ACCESS_TOKEN_PROPERTY = "portal.accesstoken";
	private static final String ENCRYPTION_ACTIVE_PROPERTY = "portal.token.encrypted";
	private static final String ENCRYPTION_ACTIVE_VALUE = "yes";
	private static final String KEYRING_ACCOUNT = "API";
	private static final String KEYRING_DOMAIN = "TestAdvisor";
	private static final String REFRESH_TOKEN_PROPERTY = "portal.refreshtoken";

	private Cipher cipher = null;
	private Properties credentials = null;
	private String passPhrase = null;
	private SecretKey secretKey = null;
	private Registry registry = null;

	public SecretsManager(Registry registry) throws IOException, TestAdvisorCipherException  {
		this(registry,false);
	}

	public SecretsManager(Registry registry, boolean cleanPassword) throws IOException, TestAdvisorCipherException  {
		this.registry = registry;
		//load credentials
		credentials = registry.getRegistryProperties();

		//clean stored password
		if (cleanPassword)
			cleanKeyStore();

		if (!isStoredInClearText() ){
			initKeyStore();
			initSecretKey();
		}
	}

	/**
	 * Initialize secret key
	 * @throws TestAdvisorCipherException
	 */
	private void initSecretKey() throws TestAdvisorCipherException {
		try{
			//SonarLint: Cipher algorithms should be robust (java:S5547)		
			cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
			byte[] passPhrase24Chars = null;
			if (passPhrase.length() >= 24)
				passPhrase24Chars = (passPhrase.substring(0, 24)).getBytes(StandardCharsets.UTF_8);
			else {
				// append whitespace to pass phrase until we reach a length of 24 chars
				passPhrase24Chars = (String.format("%-24s", passPhrase)).getBytes(StandardCharsets.UTF_8);
			}
			secretKey = SecretKeyFactory.getInstance("DESede")
					.generateSecret(new DESedeKeySpec(passPhrase24Chars));
		}catch(Exception ex){
			throw new TestAdvisorCipherException(ex);
		}
	}

	/**
	 * Saves the given access token to credentials file.
	 * 
	 * @param accessToken 
	 * access token obtained during authorization; to be stored in encrypted way
	 * @throws IOException
	 * Failed to access registry properties
	 * @throws TestAdvisorCipherException 
	 * Encryption failed for any reason
	 */
	public void setAccessToken(String accessToken) throws IOException, TestAdvisorCipherException{
		if (Strings.isNullOrEmpty(accessToken))
			throw new IllegalArgumentException("Access token is missing; authentication is required!");
		credentials.setProperty(ACCESS_TOKEN_PROPERTY, encrypt(accessToken));
		saveCredentials();
	}

	/**
	 * Gets the access token from credentials file.
	 * 
	 * @return 
	 * decrypted access token or empty string
	 * @throws TestAdvisorCipherException 
	 * if accessing credentials file or decryption failed for any reason
	 * or if access token could not be found in credentials file
	 */
	public String getAccessToken() throws TestAdvisorCipherException {
		String token = credentials.getProperty(ACCESS_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	/**
	 * Saves the given refresh token to credentials file.
	 * 
	 * @param refreshToken 
	 * refresh token obtained during authorization; to be stored in encrypted way
	 * @throws IOException
	 * Failed to access registry properties
	 * @throws TestAdvisorCipherException 
	 * Encryption failed for any reason
	 */
	public void setRefreshToken(String refreshToken) throws IOException, TestAdvisorCipherException {
		if (Strings.isNullOrEmpty(refreshToken))
			throw new IllegalArgumentException("Refresh token is missing; authentication is required!");
		credentials.setProperty(REFRESH_TOKEN_PROPERTY, encrypt(refreshToken));
		saveCredentials();
	}

	/**
	 * Gets the refresh token from credentials file.
	 * 
	 * @return 
	 * decrypted refresh token or empty string
	 * @throws TestAdvisorCipherException 
	 * if accessing credentials file or decryption failed for any reason
	 * or if refresh token could not be found in credentials file
	 */
	public String getRefreshToken() throws TestAdvisorCipherException  {
		String token = credentials.getProperty(REFRESH_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	/**
	 * 
	 * Initialize the credentials store and save a randomly generated
	 * password to this system's key store.
	 * 
	 * @throws IOException
	 * Failed to access registry properties
	 * @throws TestAdvisorCipherException
	 * This exception is thrown when failed to initialize key store 
	 */
	public void initKeyStore() throws IOException, TestAdvisorCipherException {
	    Keyring keyring = null;
		
		// initiate key store
		try {
			keyring = Keyring.create();
		} catch (BackendNotSupportedException ex) {
			throw new TestAdvisorCipherException(ex);
		}	
		
		/*
		 * We create a random string as pass phrase, store it in the local system's key store, and tell
		 * all helper methods in this class that encryption is activated. If saving the pass phrase fails,
		 * encryption is deactivated. It further assumes this client requires setup, meaning authentication
		 * and authorization. This need will be indicated in the return value.
		 */
		try {
			// obtain pass phrase from key store
			passPhrase = keyring.getPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT);
		} catch (PasswordAccessException ex) {
			// no password found
			String randomString;
			try {
				randomString = createRandomPassword();
			} catch (NoSuchAlgorithmException ex1) {
				throw new TestAdvisorCipherException(ex1);
			}
			try {
				keyring.setPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT, randomString);
				credentials.setProperty(ENCRYPTION_ACTIVE_PROPERTY, ENCRYPTION_ACTIVE_VALUE);
				saveCredentials();
				// if we get here, password is stored in key store; let's use it for encryption
				passPhrase = randomString;
			} catch (PasswordAccessException e) {
				LOGGER.log(Level.SEVERE,"Saving encryption key to local key store failed");
				LOGGER.log(Level.SEVERE,"Warning: tokens will be stored in clear text");
				throw new TestAdvisorCipherException(e);
			}
		}
	}

	private void saveCredentials() throws IOException {
		registry.saveRegistryProperties();
	}

	private String encrypt(String unencrypted) throws TestAdvisorCipherException {
		if (isStoredInClearText())
			// no encryption required
			return unencrypted;

		if (unencrypted == null)
			throw new IllegalArgumentException("String to be encrypted must not be null!");
		try{
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encryptedText = cipher.doFinal(unencrypted.getBytes(StandardCharsets.UTF_8));
			return new String(Base64.encodeBase64(encryptedText));
		}catch (Exception ex){
			throw new TestAdvisorCipherException(ex);
		}
	}

	/**
	 * remove test advisor password from key ring
	 * @throws TestAdvisorCipherException
	 * throws this exception when it failed to init key store
	 */
	private void cleanKeyStore() throws TestAdvisorCipherException {
		Keyring keyring;
		try {
			keyring = Keyring.create();
			keyring.deletePassword(KEYRING_DOMAIN, KEYRING_ACCOUNT);
		} catch (BackendNotSupportedException | PasswordAccessException ex) {
			throw new TestAdvisorCipherException(ex);
		}
	}

	private String decrypt(String encrypted) throws TestAdvisorCipherException {
		if (isStoredInClearText())
			// no decryption required
			return encrypted;

		if (encrypted == null)
			throw new IllegalArgumentException("String to be decrypted must not be null!");

		try{
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] encryptedText = Base64.decodeBase64(encrypted);
			return new String(cipher.doFinal(encryptedText));
		}catch (Exception ex){
			throw new TestAdvisorCipherException(ex);
		}
	}
	
	private boolean isStoredInClearText() {
		String encryptionActiveValue = credentials.getProperty(ENCRYPTION_ACTIVE_PROPERTY, "");
		return Strings.isNullOrEmpty(encryptionActiveValue) || !ENCRYPTION_ACTIVE_VALUE.equalsIgnoreCase(encryptionActiveValue);
	}
	
	private String createRandomPassword() throws NoSuchAlgorithmException {
	    int leftLimit = 48; // numeral '0'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;

	    Random random = SecureRandom.getInstanceStrong(); // SecureRandom is preferred to Random

	    return random.ints(leftLimit, rightLimit + 1)
					.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
					.limit(targetStringLength)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
					.toString();
	}
}
