package com.hazelcast.client.spi.impl;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import com.hazelcast.client.spi.ClientSocketFactory;

public class DefaultClientSocketFactory implements ClientSocketFactory {

	/**
	 * @see ClientSocketFactory#initialize(Properties)
	 */
	@Override
	public void init(Properties properties) {
		// Ignored
	}

	/**
	 * @see ClientSocketFactory#createSocket()
	 */
	@Override
	public Socket createSocket() throws IOException, UnknownHostException {
		return new Socket();
	}

	/**
	 * @see ClientSocketFactory#postConnectSocket(Socket)
	 */
	@Override
	public void postConnectSocket(Socket socket) {
	}

}
