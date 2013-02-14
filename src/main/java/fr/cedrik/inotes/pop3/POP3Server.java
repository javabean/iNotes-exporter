/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.inotes.util.SSLSockets;

/**
 * @author C&eacute;drik LIME
 */
public class POP3Server implements fr.cedrik.inotes.MainRunner.Main {
	private static final Logger logger = LoggerFactory.getLogger(POP3Server.class);
	public static volatile boolean shutdown = false;
	static LockOutFilter lockOutFilter;

	public POP3Server() {
	}

	public static void shutdown() {
		shutdown = true;
	}

	/**
	 * @param args
	 */
	@Override
	public void _main(String[] args) throws IOException {
		main(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		POP3Properties pop3Properties = new POP3Properties(POP3Properties.FILE);
		lockOutFilter = new LockOutFilter(pop3Properties);
		Thread pop3Thread = null, pop3sThread = null;
		if (pop3Properties.getPOP3ServerPort() >= 0) {
			//ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(pop3Properties.getPOP3ServerPort());
			ServerSocket serverSocket = new ServerSocket(pop3Properties.getPOP3ServerPort());//FIXME allow to bind to a specific interface
			pop3Thread = new ServerAcceptorThread(serverSocket);
			pop3Thread.start();
		} else {
			logger.info("No POP3 server port found in configuration!");
		}
		if (pop3Properties.getPOP3SServerPort() >= 0 && StringUtils.isNotBlank(pop3Properties.getPOP3SKeyStoreName())) {
			SSLServerSocketFactory sslServerSocketFactory = SSLSockets.getSSLServerSocketFactory(
					pop3Properties.getPOP3SKeyStoreName(), pop3Properties.getPOP3SKeyStorePassword(), pop3Properties.getPOP3SKeyStoreType(),
					pop3Properties.getPOP3STrustStoreName(), pop3Properties.getPOP3STrustStorePassword(), pop3Properties.getPOP3STrustStoreType());
			SSLServerSocket serverSSLSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(pop3Properties.getPOP3SServerPort());//FIXME allow to bind to a specific interface
			serverSSLSocket.setNeedClientAuth(false);
			pop3sThread = new ServerAcceptorThread(serverSSLSocket);
			pop3sThread.start();
		} else {
			logger.info("No POP3S server port or KeyStoreName found in configuration!");
		}
		try {
			if (pop3Thread != null) {
				pop3Thread.join();
			}
			if (pop3sThread != null) {
				pop3sThread.join();
			}
		} catch (InterruptedException ignore) {
			logger.trace("serverThread.join()", ignore);
		}
	}

	private static class ServerAcceptorThread extends Thread {
		private final ServerSocket serverSocket;
		private final boolean secure;
		public ServerAcceptorThread(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
			this.secure = (serverSocket instanceof SSLServerSocket);
		}
		@Override
		public void run() {
			logger.info("POP3{} server ready, listening on {}", (secure ? "S" : ""), serverSocket.toString());
			try {
				while (! shutdown) {
					Socket clientSocket = serverSocket.accept();
					if (lockOutFilter.isLocked(clientSocket.getInetAddress())) {
						logger.warn("An attempt was made to authenticate the locked user \"{}\"", clientSocket.getRemoteSocketAddress());
						IOUtils.closeQuietly(clientSocket);
						continue;
					}
					logger.info("New POP3{} client: {}", (secure ? "S" : ""), clientSocket.getRemoteSocketAddress());
					try {
						Thread clientThread = new Thread(new Session(clientSocket),
								"POP3"+ (secure ? "S" : "") + " client " + clientSocket.getRemoteSocketAddress());
						clientThread.setDaemon(false);
						clientThread.start();
					} catch (IOException e) {
						logger.error("Error while opening communication channels with client {}", clientSocket.getRemoteSocketAddress(), e);
						IOUtils.closeQuietly(clientSocket);
					}
				}
			} catch (IOException e) {
				logger.error("Error while listening for client connection: ", e);
			}
			logger.info("POP3{} server ({}) shutting down...", (secure ? "S" : ""), serverSocket);
			IOUtils.closeQuietly(serverSocket);
		}
	}
}
