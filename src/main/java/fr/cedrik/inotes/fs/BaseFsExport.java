/**
 *
 */
package fr.cedrik.inotes.fs;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.Folder;
import fr.cedrik.inotes.FoldersList;
import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.INotesProperties;
import fr.cedrik.inotes.Session;
import fr.cedrik.inotes.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseFsExport implements fr.cedrik.inotes.MainRunner.Main {
	protected static final String ISO8601_DATE_SEMITIME = "yyyy-MM-dd'T'HH:mm";//$NON-NLS-1$

	protected static final String PREF_LAST_EXPORT_DATE = "lastExportDate";//$NON-NLS-1$

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected INotesProperties iNotes;
	protected Session session;
	protected Date oldestMessageToFetch, newestMessageToFetch;

	public BaseFsExport() throws IOException {
	}

	protected void run(String[] args, String extension) throws IOException {
		if (args.length == 0) {
			help();
			System.exit(-1);
		}
		if (! validateDestinationName(args[0], extension)) {
			return;
		}
		iNotes = new INotesProperties(INotesProperties.FILE);
		// login
		session = new Session(iNotes);
		if (! session.login(iNotes.getUserName(), iNotes.getUserPassword())) {
			logger.error("Can not login user {}!", iNotes.getUserName());
			return;
		}
		try {
			// export folders hierarchy
			FoldersList folders = session.getFolders();
			Folder folder = folders.getFolderById(iNotes.getNotesFolderId());
			if (folder == null) {
				throw new IllegalArgumentException("Can not find folder \"" + iNotes.getNotesFolderId() + "\". Did you input the folder name instead of its id?");
			}
			export(folder, args);
		} finally {
			session.logout();
		}
	}

	protected abstract void help();

	protected void export(Folder folder, String[] args) throws IOException {
		boolean deleteExportedMessages = false;
		session.setCurrentFolder(folder);
		this.oldestMessageToFetch = null; // reset
		if (args.length > 1) {
			try {
				this.oldestMessageToFetch = new SimpleDateFormat(ISO8601_DATE_SEMITIME).parse(args[1]);
			} catch (ParseException ignore) {
				logger.warn("Bad date format: {} Please use {}", args[1], ISO8601_DATE_SEMITIME);
			}
			if (args.length > 2) {
				try {
					this.newestMessageToFetch = new SimpleDateFormat(ISO8601_DATE_SEMITIME).parse(args[2]);
					if (oldestMessageToFetch.after(newestMessageToFetch)) {
						logger.error("End date must be _after_ start date! Exiting…");
						return;
					}
				} catch (ParseException ignore) {
					logger.warn("Bad date format: {} Please use {}", args[2], ISO8601_DATE_SEMITIME);
				}
			}
			if (args.length > 3) {
				if (this.newestMessageToFetch != null && "--delete".equals(args[3])) {
					logger.warn("Flagging exported messages for deletion");
					deleteExportedMessages = true;
				}
			}
		} else if (shouldLoadOldestMessageToFetchFromPreferences()) {
			// set oldestMessageToFetch if file exists, and there is a Preference
			try {
				Preferences prefs = getUserNode(false);
				if (prefs != null) {
					long lastExportDate = prefs.getLong(PREF_LAST_EXPORT_DATE, -1);
					if (lastExportDate > 1) {// Don't incremental-export if last exported message date is null!
						this.oldestMessageToFetch = new Date(lastExportDate);
						this.newestMessageToFetch = null;
					}
				}
			} catch (BackingStoreException ignore) {
				logger.warn("Can not load last export date:", ignore);
			}
		}
		if (this.oldestMessageToFetch != null) {
			String msg = folder.name + ": incremental export from " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.oldestMessageToFetch);
			if (newestMessageToFetch != null) {
				msg += " to " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.newestMessageToFetch);
			}
			if (deleteExportedMessages) {
				msg += " with source deletion";
			}
			logger.info(msg);
		} else {
			logger.info(folder.name + ": full export");
		}
		// messages and meeting notices meta-data
		INotesMessagesMetaData<? extends BaseINotesMessage> messages = session.getMessagesAndMeetingNoticesMetaData(oldestMessageToFetch, newestMessageToFetch);
		if (folder.isInbox() || folder.isAllMails()) {
			checkQuota(messages);
		}
		if (! messages.entries.isEmpty()) {
			Date lastExportedMessageDate = this.export(messages, deleteExportedMessages);
			if (lastExportedMessageDate != null && lastExportedMessageDate.getTime() > 0) {
				if (oldestMessageToFetch != null) {
					assert lastExportedMessageDate.equals(oldestMessageToFetch) || lastExportedMessageDate.after(oldestMessageToFetch);
				}
				if (newestMessageToFetch != null) {
					assert lastExportedMessageDate.before(newestMessageToFetch) || lastExportedMessageDate.equals(newestMessageToFetch);
				}
				if (newestMessageToFetch == null) {
					// incremental export: set Preference to oldestMessageToFetch
					setPreferenceToOldestMessageToFetch(lastExportedMessageDate);
				}
			}
		}
	}

	/**
	 * @return last exported message date (can be {@code null})
	 */
	protected abstract Date export(INotesMessagesMetaData<? extends BaseINotesMessage> messages, boolean deleteExportedMessages) throws IOException;

	/**
	 * Create Java objects and store them in fields. Does not physically create files.
	 */
	protected abstract boolean validateDestinationName(String baseName, String extension);

	/**
	 * Usually, check if outFile exists
	 */
	protected abstract boolean shouldLoadOldestMessageToFetchFromPreferences();

	protected abstract void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException;

	protected void checkQuota(INotesMessagesMetaData<?> messages) {
		if (messages.ignorequota == 0 && messages.sizelimit > 0) {
			String quotaDetails = "dbsize: " + messages.dbsize
					+ " currentusage: " + messages.currentusage
					+ " warning: " + messages.warning
					+ " sizelimit: " + messages.sizelimit
					+ " ignorequota: " + messages.ignorequota;
			if (messages.dbsize >= messages.sizelimit || messages.currentusage >= messages.sizelimit) {
				logger.warn("WARNING WARNING: you have exceeded your quota! " + quotaDetails);
			} else if (messages.dbsize > messages.warning || messages.currentusage > messages.warning) {
				logger.info("WARNING: you are nearing your quota! " + quotaDetails);
			}
		}
	}

	protected void setPreferenceToOldestMessageToFetch(Date lastExportDate) {
		try {
			Preferences prefs = getUserNode(true);
			if (lastExportDate != null && lastExportDate.getTime() > 0) {
				logger.debug("Recording last export date: {} for: {}", lastExportDate, prefs);
				prefs.putLong(PREF_LAST_EXPORT_DATE, lastExportDate.getTime()+1);// +1: don't re-export last exported message next time...
			} else {
				logger.debug("Resetting (null) last export date for: {}", prefs);
				prefs.remove(PREF_LAST_EXPORT_DATE);
			}
			prefs.flush();
		} catch (BackingStoreException ignore) {
			logger.warn("Can not store last export date: " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(lastExportDate), ignore);
		}
	}

	protected Preferences getUserNode(boolean create) throws BackingStoreException {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		String key = this.getClass().getSimpleName() + '/'
				+ iNotes.getUserName().replace('/', '\\') + '@'
				+ (iNotes.getServerAddress().replace('/', '\\')) + '/'
				+ (iNotes.getNotesFolderId().replace('/', '\\'));
		if (create || prefs.nodeExists(key)) {
			return prefs.node(key);
		} else {
			return null;
		}
	}

	/**
	 * @return RFC-compliant new-line char
	 */
	protected String newLine() {
		return "\n";
	}
	/**
	 * @return RFC-compliant new-line char
	 */
	protected Writer newLine(Writer out) throws IOException {
		return out.append(newLine());
	}
}
