/**
 *
 */
package fr.cedrik.email;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import fr.cedrik.util.ExtendedProperties;

/**
 * @author C&eacute;drik LIME
 */
public abstract class EMailProperties extends ExtendedProperties {
	// email.properties keys
	private static final String SERVER   = "email.server";//$NON-NLS-1$
	private static final String USERNAME = "email.user";//$NON-NLS-1$
	private static final String PASSWORD = "email.password";//$NON-NLS-1$
	private static final String PROXY_TYPE     = "proxy.type";//$NON-NLS-1$
	private static final String PROXY_HOSTNAME = "proxy.hostname";//$NON-NLS-1$
	private static final String PROXY_PORT     = "proxy.port";//$NON-NLS-1$
	private static final String EMAIL_FOLDER_ID = "email.folder.id";//$NON-NLS-1$
	private static final String EXCLUDED_FOLDERS_IDS = "email.folder.exclude.ids";//$NON-NLS-1$


	public EMailProperties(String file) {
		init(file);
	}

	/**
	 * @param defaults
	 */
	public EMailProperties(String file, Properties defaults) {
		super(defaults);
		init(file);
	}

	/**
	 * @param defaults
	 */
	public EMailProperties(Properties defaults) {
		super(defaults);
	}

	protected void init(String file) {
//		try {
//			super.load(file);
//		} catch (IOException e) {
//			throw new IllegalStateException(e.getMessage(), e);
//		}
		InputStream in = this.getClass().getResourceAsStream(file);
		if (in != null) {
			// load from classpath
			try {
				load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Can not load file from classpath: " + file, e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		} else {
			try {
				in = new FileInputStream(new File(file));
			} catch (FileNotFoundException ignore) {
			}
			if (in != null) {
				// load from filesystem
				try {
					load(in);
				} catch (IOException e) {
					throw new IllegalStateException("Can not load file from filesystem: " + file, e);
				} finally {
					IOUtils.closeQuietly(in);
				}
			} else {
				// error
				throw new IllegalStateException("Can not find file neither in classpath nor in filesystem: " + file);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Looks up in System properties before looking up in this property list.
	 */
	@Override
	public String getProperty(String key) {
		String value = System.getProperty(key);
		if (value == null) {
			value = super.getProperty(key);
		}
		return value;
	}


	public String getServerAddress() {
		String url = getProperty(SERVER);
		if (url.endsWith("/")) {
			url = url.substring(0, url.length()-1);
		}
		return url;
	}

	public void setServerAddress(URL url) {
		setProperty(SERVER, url.toString());
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

	public abstract String getDefaultFolderId();
	public String getCurrentFolderId() {
		return getProperty(EMAIL_FOLDER_ID, getDefaultFolderId());
	}
	public void setCurrentFolderId(String folderID) {
		setProperty(EMAIL_FOLDER_ID, folderID);
	}

	public abstract String getDefaultExcludedFoldersIds();
	public List<String> getExcludedFoldersIds() {
		String idsStr = getProperty(EXCLUDED_FOLDERS_IDS, getDefaultExcludedFoldersIds());
		return Arrays.asList(StringUtils.split(idsStr, ','));
	}
}
