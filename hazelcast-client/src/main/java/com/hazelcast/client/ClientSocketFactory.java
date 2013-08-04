package com.hazelcast.client;

import java.io.IOException;
import java.net.Socket;

/**
 * 
 * @author seaniefs 04/08/2013
 *
 */
public interface ClientSocketFactory {
	public Socket createSocket() throws IOException;
}
