/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringTokenizer;

import javax.net.ssl.SSLSocket;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import fr.cedrik.inotes.pop3.commands.PASS;
import fr.cedrik.inotes.util.Charsets;

/**
 * @author C&eacute;drik LIME
 */
public class Session implements Runnable {
	private static final String CR_LF = "\r\n";//$NON-NLS-1$
	private static final String END_OF_COMMAND_RESULT = ".";//$NON-NLS-1$

	/* MDC keys */
	private static final String MDC_IP = "";
	private static final String MDC_USER = "";
	private static final String MDC_COMMAND = "";

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected final Socket clientSocket;
	protected final boolean secure;
	protected final BufferedReader in;
	protected final Writer out;
	protected final Context context ;
	protected final Map<String, POP3Command> commands = new HashMap<String, POP3Command>();

	{
		ServiceLoader<POP3Command> services = ServiceLoader.load(POP3Command.class);
		for (POP3Command command : services) {
			logger.trace("Discovered new POP3 command: {}", command.getClass().getSimpleName());
			commands.put(command.getClass().getSimpleName(), command);
		}
	}

	public Session(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		this.secure = (clientSocket instanceof SSLSocket);
		// don't inherit from server pop3Properties, as the properties will be changed per session/connected user
		this.context = new Context(clientSocket.getInetAddress(), new POP3Properties(POP3Properties.FILE));
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), DEFAULT_ENCODING));
		out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), DEFAULT_ENCODING));
	}
	private static final Charset DEFAULT_ENCODING = Charsets.forName("ISO-8859-15");

	protected String filterMultiLineResponseLine(String line) {
		if (line.startsWith(END_OF_COMMAND_RESULT)) {
			return END_OF_COMMAND_RESULT + line;
		} else {
			return line;
		}
	}

	@Override
	public void run() throws RuntimeException {
		try {
			Thread.currentThread().setName("POP3"+ (secure ? "S" : "") + " client " + clientSocket.getRemoteSocketAddress());
			MDC.put(MDC_IP, clientSocket.getRemoteSocketAddress().toString());
			out.append(ResponseStatus.POSITIVE.toString("POP3"+ (secure ? "S" : "") + " server ready")).append(CR_LF).flush();
			context.inputArgs = "";
			while (context.inputArgs != null
					&& ! clientSocket.isClosed()
					&& clientSocket.isConnected()
					&& ! clientSocket.isInputShutdown()
					&& ! clientSocket.isOutputShutdown()) {
				String inputLine = in.readLine();
				if (inputLine == null) {
					// POP3 client has broken the socket connection
					context.inputArgs = null;
					continue;
				}
				StringTokenizer tokenizer = new StringTokenizer(inputLine.trim());
				if (tokenizer.countTokens() == 0) {
					continue;
				}
				String requestedCommand = tokenizer.nextToken();
				POP3Command pop3Command = commands.get(requestedCommand.toUpperCase());
				if (pop3Command == null) {
					out.append(ResponseStatus.NEGATIVE.toString("[SYS/TEMP] " + requestedCommand)).append(CR_LF).flush();
					continue;
				}
				if (! pop3Command.isValid(context)) {
					out.append(ResponseStatus.NEGATIVE.toString("[SYS/TEMP] invalid state")).append(CR_LF).flush();
					continue;
				}
				if (context.state == State.TRANSACTION) {
					MDC.put(MDC_USER, context.userName);
				}
				if (pop3Command instanceof PASS) {
					// don't echo password in logs!
					Thread.currentThread().setName("POP3"+ (secure ? "S" : "") + " client " + clientSocket.getRemoteSocketAddress() + ' ' + requestedCommand);
					MDC.put(MDC_COMMAND, requestedCommand);
					logger.debug(requestedCommand);
				} else {
					Thread.currentThread().setName("POP3"+ (secure ? "S" : "") + " client " + clientSocket.getRemoteSocketAddress() + ' ' + inputLine);
					MDC.put(MDC_COMMAND, inputLine);
					logger.debug(inputLine);
				}
				context.inputArgs = inputLine.substring(requestedCommand.length()).trim();
				Iterator<String> responseLines;
				try {
					responseLines = pop3Command.call(context);
				} catch (IOException ioe) {
					//out.append(ResponseStatus.NEGATIVE.toString("[SYS/TEMP] " + ioe.getMessage()));
					out.append(ResponseStatus.NEGATIVE.toString("[SYS/PERM] " + ioe.getMessage()));
					throw ioe;
				}
				if (pop3Command instanceof PASS && context.state == State.TRANSACTION) {
					MDC.put(MDC_USER, context.userName);
				}
				int nLines = 0;
				while (responseLines.hasNext()) {
					String line = responseLines.next();
					assert ! line.contains("\r") : line;
					assert ! line.contains("\n") : line;
					if (nLines == 0) {
						// log only response status
						logger.debug(line);
					} else {
						logger.trace(line);
					}
					out.append(filterMultiLineResponseLine(line)).append(CR_LF);
					++nLines;
				}
				if (nLines > 1) {
					logger.trace(END_OF_COMMAND_RESULT);
					out.append(END_OF_COMMAND_RESULT).append(CR_LF);
				}
				out.flush();
				Thread.currentThread().setName("POP3"+ (secure ? "S" : "") + " client " + clientSocket.getRemoteSocketAddress());
				MDC.remove(MDC_COMMAND);
			}
		} catch (SocketTimeoutException ste) {
			logger.info("Closing socket for POP3{} client {}: {}", (secure ? "S" : ""), clientSocket.getRemoteSocketAddress(), ste.getMessage());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(clientSocket);
			MDC.clear();
		}
	}
}
