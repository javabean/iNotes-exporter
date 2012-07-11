/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author C&eacute;drik LIME
 */
public class POP3Properties extends Properties {
	private static POP3Properties INSTANCE = new POP3Properties();

	public static final String FILE = "/pop3.properties";//$NON-NLS-1$
	// pop3.properties keys
	private static final String SERVER_PORT = "port";//$NON-NLS-1$
	private static final String SHUTDOWN    = "shutdown";//$NON-NLS-1$

	/**
	 *
	 */
	public POP3Properties() {
		init();
	}

	public static POP3Properties getInstance() {
		return INSTANCE;
	}

	/**
	 * @param defaults
	 */
	public POP3Properties(Properties defaults) {
		super(defaults);
		init();
	}

	private void init() {
		InputStream in = this.getClass().getResourceAsStream(FILE);
		if (in == null) {
			throw new IllegalStateException("Can not find file " + FILE);
		} else {
			try {
				load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Can not load file " + FILE, e);
			} finally {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

	public int getServerPort() {
		return Integer.parseInt(getProperty(SERVER_PORT, "110"));
	}

	public String getShutdownSecret() {
		return getProperty(SHUTDOWN);
	}

}
