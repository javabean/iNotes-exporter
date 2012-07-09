/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * @author C&eacute;drik LIME
 */
class INotesProperties extends Properties {
	public static final String FILE = "/iNotes.properties";//$NON-NLS-1$
	public static final String DEFAULT_NOTES_FOLDER = "($Inbox)";//$NON-NLS-1$
	// iNotes.properties keys
	private static final String SERVER   = "server";//$NON-NLS-1$
	private static final String USERNAME = "user";//$NON-NLS-1$
	private static final String PASSWORD = "password";//$NON-NLS-1$
	private static final String PROXY_TYPE     = "proxy.type";//$NON-NLS-1$
	private static final String PROXY_HOSTNAME = "proxy.hostname";//$NON-NLS-1$
	private static final String PROXY_PORT     = "proxy.port";//$NON-NLS-1$
	private static final String NOTES_FOLDER_NAME = "notes.foldername";//$NON-NLS-1$

	/**
	 *
	 */
	public INotesProperties() {
		init();
	}

	/**
	 * @param defaults
	 */
	public INotesProperties(Properties defaults) {
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

	public String getServerAddress() {
		return getProperty(SERVER);
	}

	public String getUserName() {
		return getProperty(USERNAME);
	}
	public void setUserName(String userName) {
		setProperty(USERNAME, userName);
	}

	public String getUserPassword() {
		return getProperty(PASSWORD);
	}
	public void setUserPassword(String password) {
		setProperty(PASSWORD, password);
	}

	public Proxy getProxy() {
		Proxy proxy = Proxy.NO_PROXY;
		Proxy.Type type = Proxy.Type.valueOf(getProperty(PROXY_TYPE, Proxy.Type.DIRECT.name()));
		String hostname = getProperty(PROXY_HOSTNAME);

		if (type != Proxy.Type.DIRECT && StringUtils.isNotBlank(hostname)) {
			int port = Integer.parseInt(getProperty(PROXY_PORT, "-1"));//$NON-NLS-1$
			if (port < 0 || port > 65535) {
				throw new IllegalArgumentException("'port' out of range: " + port);
			}

			SocketAddress socketAddress = new InetSocketAddress(hostname, port);
			proxy = new Proxy(type, socketAddress);
		}
		return proxy;
	}

	public String getNotesFolderName() {
		return getProperty(NOTES_FOLDER_NAME, DEFAULT_NOTES_FOLDER);
	}
	public void setNotesFolderName(String notesFolderID) {
		setProperty(NOTES_FOLDER_NAME, notesFolderID);
	}
}
