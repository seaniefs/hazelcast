package com.hazelcast.client;

import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

/**
 * 
 * @author seaniefs 04/08/2013
 *
 */
public class DummyClientSocketFactory implements ClientSocketFactory {

	private static boolean	CREATED;
	
	public DummyClientSocketFactory() {
		CREATED = true;
	}

	public static void clearCreated() {
		CREATED = false;
	}

	public static boolean getCreated() {
		return CREATED;
	}

	public Socket createSocket() throws IOException {
		return SocketFactory.getDefault().createSocket();
	}

}
