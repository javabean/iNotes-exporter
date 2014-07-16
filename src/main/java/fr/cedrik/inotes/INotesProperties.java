/**
 *
 */
package fr.cedrik.inotes;

import java.util.List;
import java.util.Properties;

import fr.cedrik.email.EMailProperties;

/**
 * @author C&eacute;drik LIME
 */
public class INotesProperties extends EMailProperties {
	public static final String FILE = "/iNotes.properties";//$NON-NLS-1$
	// default values
	public static final String DEFAULT_NOTES_FOLDER_ID = Folder.INBOX;
	public static final String DEFAULT_EXCLUDED_FOLDERS_IDS = "($JunkMail),($SoftDeletions),Threads,hive,(Rules),Stationery";//$NON-NLS-1$
	public static final Boolean DEFAULT_FIX_DATE_MIME_HEADER = Boolean.TRUE;
	// iNotes.properties keys
	private static final String FIX_DATE_MIME_HEADER = "notes.mime.headers.date.fix";//$NON-NLS-1$


	public INotesProperties(String file) {
		super(file);
	}

	/**
	 * @param defaults
	 */
	public INotesProperties(String file, Properties defaults) {
		super(file, defaults);
	}

	/**
	 * @param defaults
	 */
	public INotesProperties(Properties defaults) {
		super(defaults);
	}


	@Override
	public String getDefaultFolderId() {
		return DEFAULT_NOTES_FOLDER_ID;
	}

	@Override
	public String getDefaultExcludedFoldersIds() {
		return DEFAULT_EXCLUDED_FOLDERS_IDS;
	}
	@Deprecated
	public List<String> getNotesExcludedFoldersIds() {
		return getExcludedFoldersIds();
	}

	public boolean isFixLotusNotesDateMIMEHeader() {
		return Boolean.parseBoolean(getProperty(FIX_DATE_MIME_HEADER, DEFAULT_FIX_DATE_MIME_HEADER.toString()));
	}

}
