package com.hazelcast.client.config;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import javax.net.SocketFactory;

import com.hazelcast.client.spi.ClientSocketFactory;

public class DummyClientSocketFactory implements ClientSocketFactory {

	private static boolean	CREATED;
	private static Properties PROPS;
	
	public DummyClientSocketFactory() {
		CREATED = true;
		PROPS = null;
	}
	
	@Override
	public void init(Properties properties) throws Exception {
		PROPS = properties;
	}

	public static void clearCreated() {
		CREATED = false;
	}

	public static void clearProperties() {
		PROPS = null;
	}

	public static boolean getCreated() {
		return CREATED;
	}

	public static Properties getProperties() {
		return PROPS;
	}

	@Override
	public Socket createSocket() throws IOException {
		return SocketFactory.getDefault().createSocket();
	}

	@Override
	public void postConnectSocket(Socket socket) {
		
	}

}
