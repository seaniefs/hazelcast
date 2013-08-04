/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.IOUtil;
import com.hazelcast.nio.ssl.BasicSSLContextFactory;

/**
 * @author seaniefs 04/08/2013
 */
public class ClientSocketFactoryTest {

	public static class ClassLoadingCertClientSocketFactory extends SSLClientSocketFactory {

		@Override
		protected void loadKeyStore(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
			loadCertFromClassPath(ks, passPhrase, keyStoreFile);
		}
		
	}
	
    private HazelcastInstance server;
	private HazelcastInstance hz;

	@Before
    public void setUp() {
    	DummyClientSocketFactory.clearCreated();
    }

    @After
    public void tearDown() {
    	DummyClientSocketFactory.clearCreated();
    	if(hz != null) {
    		hz.getLifecycleService().shutdown();
    	}
    	if(server != null) {
    		server.getLifecycleService().shutdown();
    	}
    }
    
    @Test
    public void testCreateSocketFactory() throws Exception {
    	ClientConfig clientConfig = new ClientConfig();
    	clientConfig.setAddresses(Arrays.asList("localhost:5901"));
    	clientConfig.setClientSocketFactory(new DummyClientSocketFactory());
    	clientConfig.setUpdateAutomatic(false);
        setupHz(clientConfig, null);
        assertTrue("Expected socketfactory to be constructed", DummyClientSocketFactory.getCreated());
    }

    @Test
    public void testCreateSSLSocketFactory() throws Exception {
    	SSLClientSocketFactory sslClientSocketFactory = createClientSslSocketFactory();
    	ClientConfig clientConfig = new ClientConfig();
    	clientConfig.setAddresses(Arrays.asList("localhost:5901"));
    	clientConfig.setClientSocketFactory(sslClientSocketFactory);
    	clientConfig.setUpdateAutomatic(false);
    	BasicSSLContextFactory sslContextFactory = new BasicSSLContextFactory() {

			@Override
			protected void loadKeyStore(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
		        loadCertFromClassPath(ks, passPhrase, keyStoreFile);
			}
    		
    	};
    	SSLConfig sslConfig = createServerSSLConfig(sslContextFactory);
        setupHz(clientConfig, sslConfig);

        System.out.println("Writing value on client...");
        hz.getMap("test").put("one", "one");
        int attempts = 10;
        Object result = null;
        do
        {
        	result = server.getMap("test").get("one");
        	if(result == null) {
            	Thread.sleep(500);
                System.out.print("Waiting for value on server...");
        	}
        }
        while(result == null && attempts-- > 0);
        System.out.println();
        assertEquals("Expected correct result!", "one", result);
        
    }

	private SSLConfig createServerSSLConfig(
			BasicSSLContextFactory sslContextFactory) {
		Properties serverSslProperties = new Properties();
    	serverSslProperties.put("keyStore", "server.ks");
    	serverSslProperties.put("keyStorePassword", "password");
    	SSLConfig sslConfig = new SSLConfig();
    	sslConfig.setEnabled(true);
    	sslConfig.setFactoryImplementation(sslContextFactory);
    	sslConfig.setProperties(serverSslProperties);
		return sslConfig;
	}

	private SSLClientSocketFactory createClientSslSocketFactory()
			throws Exception {
		Properties clientSslProperties = new Properties();
    	clientSslProperties.put("trustStore", "server.ts");
    	clientSslProperties.put("trustStorePassword", "password");
    	SSLClientSocketFactory sslClientSocketFactory = new SSLClientSocketFactory() {
			@Override
			protected void loadKeyStore(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
		        loadCertFromClassPath(ks, passPhrase, keyStoreFile);
			}
    	};
    	sslClientSocketFactory.init(clientSslProperties);
		return sslClientSocketFactory;
	}

	private void setupHz(ClientConfig clientConfig, SSLConfig sslConfig) throws Exception {
		Config config = new Config();
		config.getNetworkConfig().setPort(5901);
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		if(sslConfig != null) {
			config.getNetworkConfig().setSSLConfig(sslConfig);
 		}
        server = Hazelcast.newHazelcastInstance(config);
    	server.getMap("test").put("two", "two");
    	Thread.sleep(1000);
        hz = HazelcastClient.newHazelcastClient(clientConfig);
    	Thread.sleep(1000);
	}

	static void loadCertFromClassPath(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
		final InputStream in = ClientSocketFactoryTest.class.getResourceAsStream(keyStoreFile);
		try {
		    ks.load(in, passPhrase);
		} finally {
		    IOUtil.closeResource(in);
		}
	}
}
