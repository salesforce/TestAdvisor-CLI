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
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
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
	private static final String KEYRING_ACCOUNT = "secretkey";
	private static final String KEYRING_TEST_ACCOUNT = "test";
	private static final String KEYRING_DOMAIN = "TestAdvisor";
	private static final String REFRESH_TOKEN_PROPERTY = "portal.refreshtoken";
	private static final String ENCRYPT_ALGORITHM_STRING = "AES/GCM/NoPadding";

	private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
	private static final int AES_KEY_BIT = 256;

	private SecretKey secretKey = null;
	private Registry registry = null;
	private Keyring keyring = null;

	public SecretsManager(Registry registry) throws TestAdvisorCipherException, IOException  {
		this.registry = registry;

		if (!verifyBackend()){
			LOGGER.log(Level.WARNING, "Currently only Mac OS X, Windows and Linux (GNOME) are supported.");
			LOGGER.log(Level.WARNING, "Disable encryption.");
			registry.saveRegistryProperty(ENCRYPTION_ACTIVE_PROPERTY, ENCRYPTION_INACTIVE_VALUE);
			registry.loadRegistryProperties();
			return;
		}

		if (!isStoredInClearText() ){
			getSecretKey();
		}
	}

	public SecretsManager(Registry registry, Keyring keyring) throws TestAdvisorCipherException{
		this.registry = registry;
		this.keyring = keyring;

		if (!isStoredInClearText() ){
			getSecretKey();
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

	/**
	 * create a secret key 
	 * @throws TestAdvisorCipherException
	 */
	private SecretKey createSecretKey() throws TestAdvisorCipherException {
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(AES_KEY_BIT, SecureRandom.getInstanceStrong());
			return keyGen.generateKey();
		}catch(NoSuchAlgorithmException ex){
			throw new TestAdvisorCipherException(ex);
		}
	}

	private void getSecretKey() throws TestAdvisorCipherException{
		String encodedKey;
		try {
			encodedKey = keyring.getPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT);
		} catch (PasswordAccessException e) {
			//no key found
			secretKey = createSecretKey();
			try{
				keyring.setPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT, Base64.getEncoder().encodeToString(secretKey.getEncoded()));
			} catch (PasswordAccessException ex) {
				throw new TestAdvisorCipherException(ex);
			}
			return;
		}

		byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
		// rebuild key using SecretKeySpec
		secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");		
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

	public boolean isStoredInClearText() {
		String encryptionActiveValue = registry.getRegistryProperties().getProperty(ENCRYPTION_ACTIVE_PROPERTY, "");
		return Strings.isNullOrEmpty(encryptionActiveValue) || !ENCRYPTION_ACTIVE_VALUE.equalsIgnoreCase(encryptionActiveValue);
	}
}
