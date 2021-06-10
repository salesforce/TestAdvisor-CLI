package com.salesforce.cqe.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Strings;

import net.east301.keyring.BackendNotSupportedException;
import net.east301.keyring.Keyring;
import net.east301.keyring.PasswordRetrievalException;
import net.east301.keyring.PasswordSaveException;
import net.east301.keyring.util.LockException;

/**
 * This class providing support for handling pass phrase and access token required
 * to connect the client to the portal.
 * 
 * @author gneumann
 * @author Yibing Tao
 */
public class SecretsManager {
	private static final String ACCESS_TOKEN_PROPERTY = "portal.accesstoken";
	private static final String CREDENTIALS_FILE = "credentials.properties";
	private static final String ENCRYPTION_ACTIVE_PROPERTY = "portal.token.encrypted";
	private static final String ENCRYPTION_ACTIVE_VALUE = "yes";
	private static final String KEYRING_ACCOUNT = "API";
	private static final String KEYRING_DOMAIN = "DrillBit";
	private static final String KEYRING_FILENAME = "drillbit.crd";
	private static final String REFRESH_TOKEN_PROPERTY = "portal.refreshtoken";

	private Cipher cipher = null;
	private Properties credentials = null;
	private String passPhrase = null;
	private SecretKey secretKey = null;
	private String credentialsFileName;

	public SecretsManager() throws IOException, NoSuchAlgorithmException, 
				NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException{
		this(CREDENTIALS_FILE);
	}

	public SecretsManager(String filename) throws InvalidKeyException, NoSuchAlgorithmException, 
				NoSuchPaddingException, InvalidKeySpecException, IOException{
		
		this.credentialsFileName = filename;
		//load credentials
		credentials = new Properties();
		File credentialsFile = new File(credentialsFileName);
		if (!credentialsFile.canRead()) {
			throw new IllegalArgumentException("Failed to load credentials file:"+credentialsFileName);
		}
		try(InputStream input = new FileInputStream(credentialsFile)){
			credentials.load(input);
		}
		//init secret key
		if (isStoredInClearText())
			// encryption is deactivated
			return;

		if (secretKey != null)
			return;

		//SonarLint: Cipher algorithms should be robust (java:S5547)
		final String encryptionScheme = "AES/GCM/NoPadding";
		cipher = Cipher.getInstance(encryptionScheme);
		byte[] passPhrase24Chars = null;
		if (passPhrase.length() >= 24)
			passPhrase24Chars = (passPhrase.substring(0, 24)).getBytes(StandardCharsets.UTF_8);
		else {
			// append whitespace to pass phrase until we reach a length of 24 chars
			passPhrase24Chars = (String.format("%-24s", passPhrase)).getBytes(StandardCharsets.UTF_8);
		}
		secretKey = SecretKeyFactory.getInstance(encryptionScheme)
				.generateSecret(new DESedeKeySpec(passPhrase24Chars));
	}

	/**
	 * Saves the given access token to credentials file.
	 * 
	 * @param accessToken access token obtained during authorization; to be stored in encrypted way
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public void setAccessToken(String accessToken) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		if (Strings.isNullOrEmpty(accessToken))
			throw new IllegalArgumentException("Access token is missing; authentication is required!");
		credentials.setProperty(ACCESS_TOKEN_PROPERTY, encrypt(accessToken));
		saveCredentials();
	}

	/**
	 * Gets the access token from credentials file.
	 * 
	 * @return decrypted access token or empty string
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws Exception if accessing credentials file or decryption failed for any reason
	 * or if access token could not be found in credentials file
	 */
	public String getAccessToken() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		String token = credentials.getProperty(ACCESS_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	/**
	 * Saves the given refresh token to credentials file.
	 * 
	 * @param refreshToken refresh token obtained during authorization; to be stored in encrypted way
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws Exception if accessing credentials file or encryption failed for any reason
	 */
	public void setRefreshToken(String refreshToken) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
		if (Strings.isNullOrEmpty(refreshToken))
			throw new IllegalArgumentException("Refresh token is missing; authentication is required!");
		credentials.setProperty(REFRESH_TOKEN_PROPERTY, encrypt(refreshToken));
		saveCredentials();
	}

	/**
	 * Gets the refresh token from credentials file.
	 * 
	 * @return decrypted refresh token or empty string
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws Exception if accessing credentials file or decryption failed for any reason
	 * or if refresh token could not be found in credentials file
	 */
	public String getRefreshToken() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException  {
		String token = credentials.getProperty(REFRESH_TOKEN_PROPERTY, "");
		return (token.length() == 0) ? token : decrypt(token);
	}

	/**
	 * Finds out if the client is run for the first time on this system. If that is the case then
	 * the caller needs to authenticate and authorize this system with the DrillBit Portal app.
	 * 
	 * During this operation it will initialize the credentials file and save a randomly generated
	 * password to this system's key store.
	 * 
	 * @return true if setup, meaning authentication and authorization, is required
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws Exception if handling credentials file fails for any reason
	 */
	public boolean isSetupRequired() throws IOException, NoSuchAlgorithmException {
	    Keyring keyring = null;
		try {
			// initiate key store
			keyring = Keyring.create();
			if (keyring.isKeyStorePathRequired()) {
				// create empty file if none exists yet to avoid FileNotFoundException
				new File(KEYRING_FILENAME).createNewFile();
				keyring.setKeyStorePath(KEYRING_FILENAME);
			}
		} catch (BackendNotSupportedException ex) {
			System.err.println("Local key store not supported!");
			System.err.println("Supported OS: MacOS, Windows, GNOME");
			System.err.println("Warning: tokens will be stored in clear text");
			return true;
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
			// we are done
			return false;
		} catch (PasswordRetrievalException | LockException ex) {
			// no password found
			String randomString = createRandomPassword();
			try {
				keyring.setPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT, randomString);
				credentials.setProperty(ENCRYPTION_ACTIVE_PROPERTY, ENCRYPTION_ACTIVE_VALUE);
				saveCredentials();
				// if we get here, password is stored in key store; let's use it for encryption
				passPhrase = randomString;
			} catch (LockException | PasswordSaveException e) {
				System.err.println("Saving encryption key to local key store failed");
				System.err.println("Warning: tokens will be stored in clear text");
			}
		}
		return true;
	}

	private void saveCredentials() throws IOException {
		try(OutputStream output = new FileOutputStream(credentialsFileName)){
			// save properties to project root folder
			credentials.store(output, " !!! Do not modify directly !!!\nUse setup to update passwords and credentials!");
		}
	}

	private String encrypt(String unencrypted) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		if (isStoredInClearText())
			// no encryption required
			return unencrypted;

		if (unencrypted == null)
			throw new IllegalArgumentException("String to be encrypted must not be null!");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] encryptedText = cipher.doFinal(unencrypted.getBytes(StandardCharsets.UTF_8));
		return new String(Base64.encodeBase64(encryptedText));
	}

	private String decrypt(String encrypted) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		if (isStoredInClearText())
			// no decryption required
			return encrypted;

		if (encrypted == null)
			throw new IllegalArgumentException("String to be decrypted must not be null!");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		byte[] encryptedText = Base64.decodeBase64(encrypted);
		return new String(cipher.doFinal(encryptedText));
	}
	
	private boolean isStoredInClearText() {
		String encryptionActiveValue = credentials.getProperty(ENCRYPTION_ACTIVE_PROPERTY, "");
		return Strings.isNullOrEmpty(encryptionActiveValue) || !ENCRYPTION_ACTIVE_VALUE.equalsIgnoreCase(encryptionActiveValue);
	}
	
	private String createRandomPassword() throws NoSuchAlgorithmException {
	    int leftLimit = 48; // numeral '0'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;
		//SonarLint "Random" objects should be reused (java:S2119)
	    Random random = SecureRandom.getInstanceStrong(); // SecureRandom is preferred to Random

	    return random.ints(leftLimit, rightLimit + 1)
					.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
					.limit(targetStringLength)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
					.toString();
	}
}
