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

package com.hazelcast.nio.ssl;

import com.hazelcast.nio.IOUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

public class BasicSSLContextFactory implements SSLContextFactory {
    SSLContext sslContext;

    public BasicSSLContextFactory() {
    }

    public void init(Properties properties) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        String keyStorePassword = properties.getProperty("keyStorePassword");
        if (keyStorePassword == null) {
            keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
        }
        String keyStore = properties.getProperty("keyStore");
        if (keyStore == null) {
            keyStore = System.getProperty("javax.net.ssl.keyStore");
        }
        if (keyStore == null || keyStorePassword == null) {
            throw new RuntimeException("SSL is enabled but keyStore[Password] properties aren't set!");
        }
        String keyManagerAlgorithm = getProperty(properties, "keyManagerAlgorithm", "SunX509");
        String trustManagerAlgorithm = getProperty(properties, "trustManagerAlgorithm", "SunX509");
        String protocol = getProperty(properties, "protocol", "TLS");
        final char[] passPhrase = keyStorePassword.toCharArray();
        final String keyStoreFile = keyStore;
        loadKeyStore(ks, passPhrase, keyStoreFile);
        loadKeyStore(ts, passPhrase, keyStoreFile);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
        kmf.init(ks, passPhrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        tmf.init(ts);
        sslContext = SSLContext.getInstance(protocol);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    }

    protected void loadKeyStore(KeyStore ks, char[] passPhrase, String keyStoreFile) throws IOException, NoSuchAlgorithmException, CertificateException {
        final InputStream in = new FileInputStream(keyStoreFile);
        try {
            ks.load(in, passPhrase);
        } finally {
            IOUtil.closeResource(in);
        }
    }

    private static String getProperty(Properties properties, String propertyName, String defaultValue) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }
}
