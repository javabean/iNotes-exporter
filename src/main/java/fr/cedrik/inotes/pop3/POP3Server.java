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
		ServerSocket serverSocket = new ServerSocket(POP3Properties.getInstance().getServerPort());//FIXME allow to bind to a specific interface
		logger.info("POP3 server ready");
		while (! shutdown) {
			Socket clientSocket = serverSocket.accept();
			logger.info("New client: {}", clientSocket.getRemoteSocketAddress());
//			Thread clientThread = new Thread(new Session(clientSocket),
//					"POP3 client " + clientSocket.getRemoteSocketAddress());
//			clientThread.setDaemon(false);
//			clientThread.start();
//			try {
//				clientThread.join();// implementation is single-threaded...
//			} catch (InterruptedException ignore) {
//			}
			new Session(clientSocket).run();// implementation is single-threaded...
		}
		logger.info("POP3 server shutting down...");
		serverSocket.close();
	}

}
