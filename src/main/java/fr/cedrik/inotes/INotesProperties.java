/**
 *
 */
package fr.cedrik.inotes;

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

/**
 * @author C&eacute;drik LIME
 */
public class INotesProperties extends Properties {
	public static final String FILE = "/iNotes.properties";//$NON-NLS-1$
	// default values
	public static final String DEFAULT_NOTES_FOLDER_ID = Folder.INBOX;
	public static final String DEFAULT_EXCLUDED_FOLDERS_IDS = "($JunkMail),($SoftDeletions),Threads,hive,(Rules),Stationery";//$NON-NLS-1$
	public static final Boolean DEFAULT_FIX_DATE_MIME_HEADER = Boolean.TRUE;
	// iNotes.properties keys
	private static final String SERVER   = "inotes.server";//$NON-NLS-1$
	private static final String USERNAME = "notes.user";//$NON-NLS-1$
	private static final String PASSWORD = "notes.password";//$NON-NLS-1$
	private static final String PROXY_TYPE     = "proxy.type";//$NON-NLS-1$
	private static final String PROXY_HOSTNAME = "proxy.hostname";//$NON-NLS-1$
	private static final String PROXY_PORT     = "proxy.port";//$NON-NLS-1$
	private static final String NOTES_FOLDER_ID = "notes.folder.id";//$NON-NLS-1$
	private static final String EXCLUDED_FOLDERS_IDS = "notes.folder.exclude.ids";//$NON-NLS-1$
	private static final String FIX_DATE_MIME_HEADER = "notes.mime.headers.date.fix";//$NON-NLS-1$


	public INotesProperties(String file) {
		init(file);
	}

	/**
	 * @param defaults
	 */
	public INotesProperties(String file, Properties defaults) {
		super(defaults);
		init(file);
	}

	protected void init(String file) {
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

	public String getNotesFolderId() {
		return getProperty(NOTES_FOLDER_ID, DEFAULT_NOTES_FOLDER_ID);
	}
	public void setNotesFolderId(String notesFolderID) {
		setProperty(NOTES_FOLDER_ID, notesFolderID);
	}

	public List<String> getNotesExcludedFoldersIds() {
		String idsStr = getProperty(EXCLUDED_FOLDERS_IDS, DEFAULT_EXCLUDED_FOLDERS_IDS);
		return Arrays.asList(StringUtils.split(idsStr, ','));
	}

	public boolean isFixLotusNotesDateMIMEHeader() {
		return Boolean.parseBoolean(getProperty(FIX_DATE_MIME_HEADER, DEFAULT_FIX_DATE_MIME_HEADER.toString()));
	}

}
