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
	private static final String USERENAME_PROPERTY = "portal.username";
	private static final String PASSWORD_PROPERTY = "portal.password";
	private static final String CLIENT_ID_PROPERTY = "portal.clientid";
	private static final String CLIENT_SECRET_PROPERTY = "portal.clientsecret";
	private static final String PASSPHRASE_HASH_PROPERTY = "bcrypt.hash";
	private static final String UNICODE_CHARSET = "UTF8";

	private static Cipher cipher = null;
	private static SecretKey secretKey = null;
	private static Properties credentials = null;
	private static String passPhrase = null;

	/**
	 * Saves the given user name to credentials file.
	 * 
	 * @param userName user name to be stored in clear text
	 * @throws Exception if accessing credentials file failed for any reason
	 */
	public static void setUserName(String userName) throws Exception {
		if (Strings.isNullOrEmpty(userName))
			throw new IllegalArgumentException("Username must not be null or empty!");
		loadCredentials();
		credentials.setProperty(USERENAME_PROPERTY, userName);
		saveCredentials();
	}

	/**
	 * Gets the user name from credentials file.
	 * 
	 * @return user name or empty string if none could be found in credentials file
	 * @throws Exception if accessing credentials file failed for any reason
	 */
	public static String getUserName() throws Exception {
		loadCredentials();
		return credentials.getProperty(USERENAME_PROPERTY, "");
	}

	/**
	 * Saves the given user password to credentials file.
	 * 
	 * @param passWord password to be stored in encrypted way
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public static void setPassWord(String passWord) throws Exception {
		if (Strings.isNullOrEmpty(passWord))
			throw new IllegalArgumentException("Password must not be null or empty!");
		loadCredentials();
		initializeSecretKey();
		credentials.setProperty(PASSWORD_PROPERTY, encrypt(passWord));
		saveCredentials();
	}

	/**
	 * Gets the user password from credentials file.
	 * 
	 * @return decrypted password
	 * @throws Exception if accessing credentials file or decryption failed for any reason
	 * or if password could be found in credentials file
	 */
	public static String getPassWord() throws Exception {
		loadCredentials();
		initializeSecretKey();
		return decrypt(credentials.getProperty(PASSWORD_PROPERTY));
	}

	/**
	 * Saves the given client id, aka "Connected App ID" to credentials file.
	 * 
	 * @param clientId client ID to be stored in clear text
	 * @throws Exception if accessing credentials file failed for any reason
	 */
	public static void setClientId(String clientId) throws Exception {
		if (Strings.isNullOrEmpty(clientId))
			throw new IllegalArgumentException("Connected app ID (aka \"Client ID\") must not be null or empty!");
		loadCredentials();
		credentials.setProperty(CLIENT_ID_PROPERTY, clientId);
		saveCredentials();
	}

	/**
	 * Gets the client ID from credentials file.
	 * 
	 * @return client ID or empty string if none could be found in credentials file
	 * @throws Exception if accessing credentials file failed for any reason
	 */
	public static String getClientId() throws Exception {
		loadCredentials();
		return credentials.getProperty(CLIENT_ID_PROPERTY, "");
	}

	/**
	 * Saves the given client secret aka "Connected App Secret" to credentials file.
	 * 
	 * @param clientSecret client secret to be stored in encrypted way
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public static void setClientSecret(String clientSecret) throws Exception {
		if (Strings.isNullOrEmpty(clientSecret))
			throw new IllegalArgumentException("Connected app secrett(aka \"Client Secret\") must not be null or empty!");
		loadCredentials();
		initializeSecretKey();
		credentials.setProperty(CLIENT_SECRET_PROPERTY, encrypt(clientSecret));
		saveCredentials();
	}

	/**
	 * Gets the client secret from credentials file.
	 * 
	 * @return decrypted client secret
	 * @throws Exception if accessing credentials file or decryption failed for any reason
	 * or if client secret could be found in credentials file
	 */
	public static String getClientSecret() throws Exception {
		loadCredentials();
		initializeSecretKey();
		return decrypt(credentials.getProperty(CLIENT_SECRET_PROPERTY));
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
