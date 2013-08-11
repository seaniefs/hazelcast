package com.hazelcast.client.spi;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public interface ClientSocketFactory {

	public void init(Properties properties) throws Exception;
	public Socket createSocket() throws IOException;
	public void postConnectSocket(Socket socket) throws IOException;

}
