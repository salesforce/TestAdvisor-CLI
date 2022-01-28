/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cte.testadvisor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
	private static final String ENCRYPTION_INACTIVE_VALUE = "no";
	private static final String KEYRING_ACCOUNT = "API";
	private static final String KEYRING_TEST_ACCOUNT = "API.TEST";
	private static final String KEYRING_DOMAIN = "TestAdvisor";
	private static final String REFRESH_TOKEN_PROPERTY = "portal.refreshtoken";
	private static final String ENCRYPT_ALGORITHM_STRING = "AES/GCM/NoPadding";

	private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
	private static final int AES_KEY_BIT = 256;
	private static final int AES_INTERATION_COUNT = 65536;


	private String passPhrase = null;
	private SecretKey secretKey = null;
	private Registry registry = null;
	private Keyring keyring = null;

	public SecretsManager(Registry registry) throws TestAdvisorCipherException, IOException  {
		this.registry = registry;

		if (!verifyBackend()){
			LOGGER.log(Level.WARNING, "Currently only Mac OS X, Windows and Linux (GNOME) are supported.");
			LOGGER.log(Level.WARNING, "Disable encryption.");
			registry.saveRegistryProperty(ENCRYPTION_ACTIVE_PROPERTY, ENCRYPTION_INACTIVE_VALUE);
			return;
		}

		getPassPhrase();
		
		if (!isStoredInClearText() ){
			createSecretKey();
		}
	}

	public SecretsManager(Registry registry, String passphrase) throws IOException, TestAdvisorCipherException{
		this.registry = registry;

		if (!verifyBackend()){
			LOGGER.log(Level.WARNING, "Currently only Mac OS X, Windows and Linux (GNOME) are supported.");
			LOGGER.log(Level.WARNING, "Disable encryption.");
			registry.saveRegistryProperty(ENCRYPTION_ACTIVE_PROPERTY, ENCRYPTION_INACTIVE_VALUE);
			return;
		}

		setPassPhrase(passphrase);
		
		if (!isStoredInClearText() ){
			createSecretKey();
		}
	}

	/**
	 * Check if the backend supports secure key store
	 * @return
	 * true if the backend supports secure key store
	 * false otherwise
	 */
	private boolean verifyBackend(){
		try {
			keyring = Keyring.create();
		} catch (BackendNotSupportedException e) {
			LOGGER.log(Level.WARNING,"Current backend does NOT support native OS key store.");
			return false;
		}
		try {
			keyring.setPassword(KEYRING_DOMAIN, KEYRING_TEST_ACCOUNT, "passPhrase");
			keyring.getPassword(KEYRING_DOMAIN, KEYRING_TEST_ACCOUNT);
			keyring.deletePassword(KEYRING_DOMAIN, KEYRING_TEST_ACCOUNT);
		} catch (PasswordAccessException e) {
			LOGGER.log(Level.WARNING,"Saving/Retriving key to/from native OS key store failed.");
			return false;
		}

		return true;
	}

	private void setPassPhrase(String passPhrase) throws TestAdvisorCipherException {
		try {
			keyring.setPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT, passPhrase);
			this.passPhrase = passPhrase;
		} catch (PasswordAccessException e) {
			throw new TestAdvisorCipherException(e);
		}
	}

	private String getPassPhrase() throws TestAdvisorCipherException {
		try {
			passPhrase = keyring.getPassword(KEYRING_DOMAIN, KEYRING_TEST_ACCOUNT);
			return passPhrase;
		} catch (PasswordAccessException e) {
			// no passpharse found, create a random one
			passPhrase = createRandomPassword();
			setPassPhrase(passPhrase);
			return passPhrase;
		}
	}

	/**
	 * Initialize secret key
	 * @throws TestAdvisorCipherException
	 */
	private void createSecretKey() throws TestAdvisorCipherException {
		try{
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			// iterationCount = 65536
			// keyLength = 256
			byte[] salt = new byte[IV_LENGTH_BYTE];
			new SecureRandom().nextBytes(salt);
			KeySpec spec = new PBEKeySpec(passPhrase.toCharArray(), salt, AES_INTERATION_COUNT, AES_KEY_BIT);
			secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
		}catch(InvalidKeySpecException | NoSuchAlgorithmException ex){
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
		registry.saveRegistryProperty(ACCESS_TOKEN_PROPERTY, encrypt(accessToken));
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
		String token = registry.getRegistryProperties().getProperty(ACCESS_TOKEN_PROPERTY, "");
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
		registry.saveRegistryProperty(REFRESH_TOKEN_PROPERTY, encrypt(refreshToken));
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
		String token = registry.getRegistryProperties().getProperty(REFRESH_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	private String encrypt(String unencrypted) throws TestAdvisorCipherException {
		if (isStoredInClearText())
			// no encryption required
			return unencrypted;

		byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(ENCRYPT_ALGORITHM_STRING);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			byte[] encryptedText = cipher.doFinal(unencrypted.getBytes());
			//add IV to head of encrypted string
			return bytesToString(iv) + bytesToString(encryptedText);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException 
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new TestAdvisorCipherException(e);
		}
			
	}

	private String decrypt(String encrypted) throws TestAdvisorCipherException {
		if (isStoredInClearText())
			// no decryption required
			return encrypted;

		if (encrypted == null)
			throw new IllegalArgumentException("String to be decrypted must not be null!");

		byte[] encryptedBytes = stringToBytes(encrypted);
		ByteBuffer bb = ByteBuffer.wrap(encryptedBytes);

		//read IV first
		byte[] iv = new byte[IV_LENGTH_BYTE];
		bb.get(iv);

		//read actural encrypted text
		byte[] cipherText = new byte[bb.remaining()];
		bb.get(cipherText);
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(ENCRYPT_ALGORITHM_STRING);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			byte[] plainText = cipher.doFinal(cipherText);
			return new String(plainText, StandardCharsets.UTF_8);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException 
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new TestAdvisorCipherException(e);
		}
	}

	public String bytesToString(byte[] bytes){
		return Base64.getEncoder().encodeToString(bytes);
	}

	public byte[] stringToBytes(String string){
		return Base64.getDecoder().decode(string);
	}

	private boolean isStoredInClearText() {
		String encryptionActiveValue = registry.getRegistryProperties().getProperty(ENCRYPTION_ACTIVE_PROPERTY, "");
		return Strings.isNullOrEmpty(encryptionActiveValue) || !ENCRYPTION_ACTIVE_VALUE.equalsIgnoreCase(encryptionActiveValue);
	}
	
	private String createRandomPassword() throws TestAdvisorCipherException {
	    int leftLimit = 48; // numeral '0'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;

	    Random random;
		try {
			random = SecureRandom.getInstanceStrong();
			return random.ints(leftLimit, rightLimit + 1)
					.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
					.limit(targetStringLength)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
					.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new TestAdvisorCipherException(e);
		} 

	    
	}
}
