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

package com.hazelcast.client.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.spi.impl.SSLClientSocketFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.IOUtil;
import com.hazelcast.nio.ssl.BasicSSLContextFactory;
import com.hazelcast.test.HazelcastJUnit4ClassRunner;

/**
 * @author ali 5/20/13
 */
@RunWith(HazelcastJUnit4ClassRunner.class)
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
    	DummyClientSocketFactory.clearProperties();
    }

    @After
    public void tearDown() {
    	DummyClientSocketFactory.clearCreated();
    	DummyClientSocketFactory.clearProperties();
    	if(hz != null) {
    		hz.getLifecycleService().shutdown();
    	}
    	if(server != null) {
    		server.getLifecycleService().shutdown();
    	}
    }
    
    @Test
    public void testCreateSocketFactory() throws Exception {
    	String strClientConfig = "hazelcast-client-socketfactory.xml";
        setupHz(strClientConfig, null);
        assertTrue("Expected socketfactory to be constructed", DummyClientSocketFactory.getCreated());
    }

    @Test
    public void testCreateSocketFactoryWithProps() throws Exception {
    	String strClientConfig = "hazelcast-client-socketfactory-props.xml";
        setupHz(strClientConfig, null);
        Properties properties = DummyClientSocketFactory.getProperties();
        assertTrue("Expected properties to be applied", properties != null);
        assertEquals("Expected all property one to be set ", "one", properties.get("one"));
        assertEquals("Expected all property two to be set ", "two", properties.get("two"));
        assertEquals("Expected all property three to be set ", "three", properties.get("three"));
    }

    @Test
    public void testCreateSSLSocketFactory() throws Exception {
    	Properties properties = new Properties();
    	properties.put("keyStore", "server.ks");
    	properties.put("keyStorePassword", "password");
    	BasicSSLContextFactory sslContextFactory = new BasicSSLContextFactory() {

			@Override
			protected void loadKeyStore(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
		        loadCertFromClassPath(ks, passPhrase, keyStoreFile);
			}
    		
    	};
    	sslContextFactory.init(properties);
    	SSLConfig sslConfig = new SSLConfig();
    	sslConfig.setEnabled(true);
    	sslConfig.setFactoryImplementation(sslContextFactory);
    	sslConfig.setProperties(properties);
    	String strClientConfig = "hazelcast-client-socketfactory-ssl.xml";
        setupHz(strClientConfig, sslConfig);

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

	private void setupHz(String strClientConfig, SSLConfig sslConfig) throws Exception {
		Config config = new Config();
		config.getNetworkConfig().setPort(5901);
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		if(sslConfig != null) {
			config.getNetworkConfig().setSSLConfig(sslConfig);
 		}
        server = Hazelcast.newHazelcastInstance(config);
    	server.getMap("test").put("two", "two");
        ClientConfig clientConfig = new XmlClientConfigBuilder(getClass().getClassLoader().getResourceAsStream(strClientConfig)).build();
        hz = HazelcastClient.newHazelcastClient(clientConfig);
	}

	static void loadCertFromClassPath(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
		final InputStream in = ClientSocketFactoryTest.class.getClassLoader().getResourceAsStream(keyStoreFile);
		try {
		    ks.load(in, passPhrase);
		} finally {
		    IOUtil.closeResource(in);
		}
	}
}
