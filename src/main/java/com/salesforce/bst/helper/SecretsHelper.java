/**
 * 
 */
package com.salesforce.bst.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.mindrot.jbcrypt.BCrypt;

import com.google.common.base.Strings;

/**
 * Helper class providing support for handling all secrets and tokens required
 * to connect the client to the portal.
 * 
 * @author gneumann
 */
public class SecretsHelper {
	private static final String CREDENTIALS_FILE = "credentials.properties";
	private static final String ACCESS_TOKEN_PROPERTY = "portal.accesstoken";
	private static final String REFRESH_TOKEN_PROPERTY = "portal.refreshtoken";
	private static final String PASSPHRASE_HASH_PROPERTY = "bcrypt.hash";
	private static final String UNICODE_CHARSET = "UTF8";

	private static Cipher cipher = null;
	private static SecretKey secretKey = null;
	private static Properties credentials = null;
	private static String passPhrase = null;

	/**
	 * Saves the given access token to credentials file.
	 * 
	 * @param accessToken access token obtained during authorization; to be stored in encrypted way
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public static void setAccessToken(String accessToken) throws Exception {
		if (Strings.isNullOrEmpty(accessToken))
			throw new IllegalArgumentException("Access token is missing; authentication is required!");
		loadCredentials();
		initializeSecretKey();
		credentials.setProperty(ACCESS_TOKEN_PROPERTY, encrypt(accessToken));
		saveCredentials();
	}

	/**
	 * Clears the given access token by setting it to empty string.
	 * 
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public static void clearAccessToken() throws Exception {
		loadCredentials();
		initializeSecretKey();
		credentials.setProperty(ACCESS_TOKEN_PROPERTY, "");
		saveCredentials();
	}

	/**
	 * Gets the access token from credentials file.
	 * 
	 * @return decrypted access token or empty string
	 * @throws Exception if accessing credentials file or decryption failed for any reason
	 * or if access token could not be found in credentials file
	 */
	public static String getAccessToken() throws Exception {
		loadCredentials();
		initializeSecretKey();
		String token = credentials.getProperty(ACCESS_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	/**
	 * Saves the given refresh token to credentials file.
	 * 
	 * @param refreshToken refresh token obtained during authorization; to be stored in encrypted way
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public static void setRefreshToken(String refreshToken) throws Exception {
		if (Strings.isNullOrEmpty(refreshToken))
			throw new IllegalArgumentException("Refresh token is missing; authentication is required!");
		loadCredentials();
		initializeSecretKey();
		credentials.setProperty(REFRESH_TOKEN_PROPERTY, encrypt(refreshToken));
		saveCredentials();
	}

	/**
	 * Clears the given refresh token by setting it to empty string.
	 * 
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public static void clearRefreshToken() throws Exception {
		loadCredentials();
		initializeSecretKey();
		credentials.setProperty(REFRESH_TOKEN_PROPERTY, "");
		saveCredentials();
	}

	/**
	 * Gets the refresh token from credentials file.
	 * 
	 * @return decrypted refresh token or empty string
	 * @throws Exception if accessing credentials file or decryption failed for any reason
	 * or if refresh token could not be found in credentials file
	 */
	public static String getRefreshToken() throws Exception {
		loadCredentials();
		initializeSecretKey();
		String token = credentials.getProperty(REFRESH_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	/**
	 * Sets the given pass phrase.
	 * 
	 * It will hash the pass phrase using Bcrypt and compare it with the hash saved to
	 * credentials file during setup. If this hash matches, the pass phrase is considered
	 * valid.
	 * 
	 * During setup the pass phrase will get hashed using Bcrypt algorithm. The resulting
	 * hash code is written to the credentials file.
	 *  
	 * @param passPhrase pass phrase to be used for de- and encryption of passwords and secrets
	 * @throws Exception if this pass phrase's hash does not match the previously saved hash. 
	 * It is also thrown when accessing credentials file or hashing failed for any reason.
	 */
	public static void setPassPhrase(final String passPhrase, boolean isSetup) throws Exception {
		if (Strings.isNullOrEmpty(passPhrase))
			throw new IllegalStateException("Passphrase must not be null or empty!");

		loadCredentials();
		SecretsHelper.passPhrase = passPhrase;
		if (isSetup) {
			credentials.setProperty(PASSPHRASE_HASH_PROPERTY, BCrypt.hashpw(passPhrase, BCrypt.gensalt()));
			saveCredentials();
			System.out.println("Success: pass phrase has been set!");
			System.out
					.println("Make sure to remember it going forward or information stored in key store will be lost.");
			return;
		}

		// only pass phrase validation is required
		boolean isMatch = BCrypt.checkpw(passPhrase, credentials.getProperty(PASSPHRASE_HASH_PROPERTY, ""));
		if (isMatch) {
			System.out.println("Success: pass phrase matches!");
		} else {
			SecretsHelper.passPhrase = null;
			throw new Exception("Failure: pass phrase does NOT match!");
		}
	}

	private static void loadCredentials() throws Exception {
		if (credentials != null)
			return;

		credentials = new Properties();
		File credentialsFile = new File(CREDENTIALS_FILE);
		if (credentialsFile.canRead()) {
			// file exists
			InputStream input = new FileInputStream(credentialsFile);
			credentials.load(input);
		}
	}

	private static void saveCredentials() throws Exception {
		OutputStream output = new FileOutputStream(CREDENTIALS_FILE);
		// save properties to project root folder
		credentials.store(output, " !!! Do not modify directly !!!\nUse setup to update passwords and credentials!");
	}

	private static void initializeSecretKey() throws Exception {
		if (secretKey != null)
			return;
		final String encryptionScheme = "DESede";
		cipher = Cipher.getInstance(encryptionScheme);
		byte[] passPhrase24Chars = null;
		if (passPhrase.length() >= 24)
			passPhrase24Chars = (passPhrase.substring(0, 24)).getBytes(UNICODE_CHARSET);
		else {
			// append whitespace to pass phrase until we reach a length of 24 chars
			passPhrase24Chars = (String.format("%-24s", passPhrase)).getBytes(UNICODE_CHARSET);
		}
		secretKey = SecretKeyFactory.getInstance(encryptionScheme)
				.generateSecret(new DESedeKeySpec(passPhrase24Chars));
	}

	private static String encrypt(String unencrypted) throws Exception {
		if (unencrypted == null)
			throw new IllegalArgumentException("String to be encrypted must not be null!");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] encryptedText = cipher.doFinal(unencrypted.getBytes(UNICODE_CHARSET));
		return new String(Base64.encodeBase64(encryptedText));
	}

	private static String decrypt(String encrypted) throws Exception {
		if (encrypted == null)
			throw new IllegalArgumentException("String to be decrypted must not be null!");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		byte[] encryptedText = Base64.decodeBase64(encrypted);
		return new String(cipher.doFinal(encryptedText));
	}
}
