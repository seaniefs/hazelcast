package com.hazelcast.client.config;

import java.util.Properties;

import com.hazelcast.client.spi.ClientSocketFactory;
import com.hazelcast.client.spi.impl.DefaultClientSocketFactory;

public class ClientSocketFactoryOptions {

	private String 					factoryClassName;
	private Properties				socketFactoryProperties = new Properties();
	private ClientSocketFactory		clientSocketFactoryImplementation = new DefaultClientSocketFactory();
	
	public void setFactoryClassName(String factoryClassName) {
		this.factoryClassName = factoryClassName;
	}

	public String getFactoryClassName() {
		return factoryClassName;
	}

	public Properties getProperties() {
		return socketFactoryProperties;
	}

	public ClientSocketFactory getClientSocketFactoryImplementation() {
		return clientSocketFactoryImplementation;
	}

	public void setClientSocketFactory(ClientSocketFactory clientSocketFactory) {
		this.clientSocketFactoryImplementation = clientSocketFactory;
	}

}
