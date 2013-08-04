package com.hazelcast.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.hazelcast.nio.IOUtil;

/**
 * 
 * @author seaniefs 04/08/2013
 *
 */
public class SSLClientSocketFactory implements ClientSocketFactory {

	private class KeyHelper {

		private String		storeType;
		private char[]		passPhrase;
		private String		keyStoreLocation;
		private Properties properties;
		private boolean   mandatory;

		public KeyHelper(String storeType, Properties properties, boolean mandatory) {
			this.storeType = storeType;
			this.properties = properties;
			this.mandatory = mandatory;
		}

	    public KeyStore initializeKeyStore(String defaultLocation, char[] defaultPassPhrase) throws Exception {

	    	KeyStore keyStore = null;

	        keyStoreLocation = properties.getProperty(storeType);
	        if (keyStoreLocation == null) {
	            keyStoreLocation = System.getProperty("javax.net.ssl." + storeType);
	        }

	        keyStoreLocation = keyStoreLocation == null ? defaultLocation : keyStoreLocation;
	        
	        String keyStorePassword = properties.getProperty(storeType + "Password");

	        if (keyStorePassword == null) {
	            keyStorePassword = System.getProperty("javax.net.ssl." + storeType + "Password");
	        }

	        passPhrase = keyStorePassword != null ? keyStorePassword.toCharArray() : defaultPassPhrase;

	        if ( keyStoreLocation == null || passPhrase == null ) {
	        	if(mandatory) {
	        		throw new RuntimeException("SSL is enabled but " + storeType + "[Password] properties aren't set!");
	        	}
	        }
	        else {
		    	keyStore = KeyStore.getInstance("JKS");
	        	loadKeyStore(keyStore, passPhrase, keyStoreLocation);
	        }

	        return keyStore;
	    }

	    public char[] getPassPhrase() {
			return passPhrase;
		}

		public String getKeyStoreLocation() {
			return keyStoreLocation;
		}

	}

	private SSLSocketFactory socketFactory;

    public void init(Properties properties) throws Exception {

        String protocol = getProperty(properties, "protocol", "TLS");

        // Keystore on client is optional, but truststore isn't...
    	KeyHelper keyHelper = new KeyHelper("keyStore", properties, false);
    	KeyManager[] keyManagers = null;

    	KeyStore ks = keyHelper.initializeKeyStore(null, null);

    	if(ks != null) {
            String keyManagerAlgorithm = getProperty(properties, "keyManagerAlgorithm", "SunX509");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
            kmf.init(ks, keyHelper.getPassPhrase());
            keyManagers = kmf.getKeyManagers();
    	}

    	KeyHelper trustHelper = new KeyHelper("trustStore", properties, true);
    	KeyStore ts = trustHelper.initializeKeyStore(keyHelper.getKeyStoreLocation(), keyHelper.getPassPhrase());
    	
        String trustManagerAlgorithm = getProperty(properties, "trustManagerAlgorithm", "SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        tmf.init(ts);

        SSLContext sslContext = SSLContext.getInstance(protocol);
		TrustManager[] trustManagers = tmf.getTrustManagers();
		sslContext.init(keyManagers, trustManagers, null);
        socketFactory = sslContext.getSocketFactory();
    }

	/**
	 * @see ClientSocketFactory#createSocket()
	 */
	public Socket createSocket() throws IOException {
		return socketFactory.createSocket();
	}

	/**
     * Used to load the keystore - can be overridden to allow loading from classpath, etc
     * @param ks
     * @param passPhrase
     * @param keyStoreFile
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
	protected void loadKeyStore(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
        final InputStream in = new FileInputStream(keyStoreFile);
        try {
            ks.load(in, passPhrase);
        } finally {
            IOUtil.closeResource(in);
        }
    }

    private String getProperty(Properties properties, String propertyName, String defaultValue) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
	
}