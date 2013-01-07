/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		ServerSocket serverSocket = new ServerSocket(pop3Properties.getPOP3ServerPort());//FIXME allow to bind to a specific interface
		logger.info("POP3 server ready, listening on " + serverSocket.toString());
		while (! shutdown) {
			Socket clientSocket = serverSocket.accept();
			if (lockOutFilter.isLocked(clientSocket.getInetAddress())) {
				logger.warn("An attempt was made to authenticate the locked user \"{}\"", clientSocket.getRemoteSocketAddress());
				clientSocket.close();
				continue;
			}
			logger.info("New client: {}", clientSocket.getRemoteSocketAddress());
			Thread clientThread = new Thread(new Session(clientSocket),
					"POP3 client " + clientSocket.getRemoteSocketAddress());
			clientThread.setDaemon(false);
			clientThread.start();
		}
		logger.info("POP3 server shutting down...");
		serverSocket.close();
	}

}
