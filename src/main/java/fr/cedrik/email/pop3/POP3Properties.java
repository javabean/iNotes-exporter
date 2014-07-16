/**
 *
 */
package fr.cedrik.email.pop3;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.Properties;

import fr.cedrik.util.ExtendedProperties;

/**
 * @author C&eacute;drik LIME
 */
public class POP3Properties extends ExtendedProperties {
	// default values
	public static final String DEFAULT_PORT = "110";//$NON-NLS-1$
	public static final String DEFAULT_S_PORT = "995";//$NON-NLS-1$
	public static final String DEFAULT_SHARED_SECRET = "";//$NON-NLS-1$
	public static final String DEFAULT_S_STORETYPE = "PKCS12";//$NON-NLS-1$
	public static final int    DEFAULT_SO_TIMEOUT_SECONDS = 60;
	// additional keys
	private static final String SHUTDOWN_SECRET = "pop3.shutdown";//$NON-NLS-1$
	private static final String SERVER_PORT     = "pop3.port";//$NON-NLS-1$
	private static final String SERVER_S_PORT               = "pop3s.port";//$NON-NLS-1$
	private static final String SERVER_S_keyStoreName       = "pop3s.keyStoreName";//$NON-NLS-1$
	private static final String SERVER_S_keyStorePassword   = "pop3s.keyStorePassword";//$NON-NLS-1$
	private static final String SERVER_S_keyStoreType       = "pop3s.keyStoreType";//$NON-NLS-1$
	private static final String SERVER_S_keyPassword        = "pop3s.keyPassword";//$NON-NLS-1$
	private static final String SERVER_S_trustStoreName     = "pop3s.trustStoreName";//$NON-NLS-1$
	private static final String SERVER_S_trustStorePassword = "pop3s.trustStorePassword";//$NON-NLS-1$
	private static final String SERVER_S_trustStoreType     = "pop3s.trustStoreType";//$NON-NLS-1$
	private static final String SO_TIMEOUT      = "pop3.socket.timeout";//$NON-NLS-1$

	private final String file;

	/**
	 *
	 */
	public POP3Properties(String file) {
		super();
		this.file = file;
		try {
			load(file);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param defaults
	 */
	public POP3Properties(String file, Properties defaults) {
		super(defaults);
		this.file = file;
		try {
			load(file);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param defaults
	 */
	public POP3Properties(Properties defaults) {
		super(defaults);
		this.file = null;
	}

	public int getPOP3ServerPort() {
		return Integer.parseInt(getProperty(SERVER_PORT, DEFAULT_PORT));
	}

	public int getPOP3SServerPort() {
		return Integer.parseInt(getProperty(SERVER_S_PORT, DEFAULT_S_PORT));
	}

	public String getPOP3SKeyStoreName() {
		return getProperty(SERVER_S_keyStoreName);
	}

	public String getPOP3SKeyStorePassword() {
		return getProperty(SERVER_S_keyStorePassword);
	}

	public String getPOP3SKeyStoreType() {
		return getProperty(SERVER_S_keyStoreType, DEFAULT_S_STORETYPE);
	}

	public String getPOP3SKeyPassword() {
		return getProperty(SERVER_S_keyPassword);
	}

	public String getPOP3STrustStoreName() {
		return getProperty(SERVER_S_trustStoreName);
	}

	public String getPOP3STrustStorePassword() {
		return getProperty(SERVER_S_trustStorePassword);
	}

	public String getPOP3STrustStoreType() {
		return getProperty(SERVER_S_trustStoreType, DEFAULT_S_STORETYPE);
	}

	public String getPOP3ShutdownSecret() {
		return getProperty(SHUTDOWN_SECRET, DEFAULT_SHARED_SECRET);
	}

	/**
	 * @see java.net.Socket#getSoTimeout()
	 */
	public int getPOP3soTimeout() throws NumberFormatException {
		int seconds = Integer.parseInt(getProperty(SO_TIMEOUT, Integer.toString(DEFAULT_SO_TIMEOUT_SECONDS)));
		return (int) SECONDS.toMillis(Math.max(0, seconds));
	}

}
