/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.util.Properties;

import fr.cedrik.inotes.INotesProperties;

/**
 * @author C&eacute;drik LIME
 */
public class POP3Properties extends INotesProperties {
	// default values
	public static final String DEFAULT_PORT = "110";//$NON-NLS-1$
	public static final String DEFAULT_SHARED_SECRET = "";//$NON-NLS-1$
	// additional keys
	private static final String SERVER_PORT = "pop3.port";//$NON-NLS-1$
	private static final String SHUTDOWN_SECRET = "pop3.shutdown";//$NON-NLS-1$

	/**
	 *
	 */
	public POP3Properties(String file) {
		super(file);
	}

	/**
	 * @param defaults
	 */
	public POP3Properties(String file, Properties defaults) {
		super(file, defaults);
	}

	@Override
	protected void init(String file) {
		super.init(file);
		setNotesFolderId(DEFAULT_NOTES_FOLDER_ID);
	}

	public int getPOP3ServerPort() {
		return Integer.parseInt(getProperty(SERVER_PORT, DEFAULT_PORT));
	}

	public String getPOP3ShutdownSecret() {
		return getProperty(SHUTDOWN_SECRET, DEFAULT_SHARED_SECRET);
	}

}
