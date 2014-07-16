/**
 *
 */
package fr.cedrik.email.fs;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailParseException;

import fr.cedrik.email.EMailProperties;
import fr.cedrik.email.FoldersList;
import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.spi.Message;
import fr.cedrik.email.spi.PropertiesFileSupplier;
import fr.cedrik.email.spi.Session;
import fr.cedrik.email.spi.SessionSupplier;
import fr.cedrik.inotes.Folder;
import fr.cedrik.util.DateUtils;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseFsExport implements fr.cedrik.email.MainRunner.Main {
	protected static final String ISO8601_DATE_SEMITIME = "yyyy-MM-dd'T'HH:mm";//$NON-NLS-1$

	protected static final String PREF_LAST_EXPORT_DATE = "lastExportDate";//$NON-NLS-1$

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected EMailProperties email;
	protected Session session;
	protected Date oldestMessageToFetch, newestMessageToFetch;
	protected BaseFileWriter writer;

	public BaseFsExport() throws IOException {
	}

	protected void run(String[] args, String extension) throws IOException {
		if (args.length == 0) {
			help();
			System.exit(-1);
		}
		if (! prepareDestinationObjects(args[0], extension)) {
			return;
		}
		email = PropertiesFileSupplier.Util.get();
		// login
		session = SessionSupplier.Util.get(email);
		if (! session.login(email.getUserName(), email.getUserPassword())) {
			logger.error("Can not login user {}!", email.getUserName());
			return;
		}
		try {
			// export folders hierarchy
			FoldersList folders = session.getFolders();
			Folder folder = folders.getFolderById(email.getCurrentFolderId());
			if (folder == null) {
				throw new IllegalArgumentException("Can not find folder \"" + email.getCurrentFolderId() + "\". Did you input the folder name instead of its id?");
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
			String msg = folder.getName() + ": incremental export from " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.oldestMessageToFetch);
			if (newestMessageToFetch != null) {
				msg += " to " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.newestMessageToFetch);
			}
			if (deleteExportedMessages) {
				msg += " with source deletion";
			}
			logger.info(msg);
		} else {
			logger.info(folder.getName() + ": full export");
		}
		// messages and meeting notices meta-data
		MessagesMetaData<? extends Message> messages = session.getMessagesMetaData(oldestMessageToFetch, newestMessageToFetch);
		if (folder.isInbox() || folder.isAllMails()) {
			checkQuota(messages);
		}
		if (! messages.entries.isEmpty()) {
			final boolean append = writer.exists() && (oldestMessageToFetch != null);
			Date lastExportedMessageDate = this.export(messages, append, deleteExportedMessages);
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
	protected final Date export(MessagesMetaData<? extends Message> messages, boolean append, boolean deleteExportedMessages) throws IOException {
		Date lastExportedMessageDate = null;
		try {
			writer.openFolder(append);
			// write messages
			List<Message> writtenMessages = new ArrayList<Message>();
			for (Message message : messages.entries) {
				IteratorChain<String> mime;
				try {
					mime = session.getMessageMIME(message);
				} catch (MailParseException mpe) {
					mime = null;
				}
				if (mime == null || ! mime.hasNext()) {
					logger.error("Empty MIME message! ({})", message);
					IOUtils.closeQuietly(mime);
					break;
				}
				try {
					writer.openFile(message, append);
					writer.write(message, mime);
					writtenMessages.add(message);
				} finally {
					IOUtils.closeQuietly(mime);
					writer.closeFile(message);
				}
				if (message.getDate().getTime() > 0) {
					lastExportedMessageDate = message.getDate();
				}
			}
			writer.flush();
			if (deleteExportedMessages) {
				session.deleteMessage(writtenMessages);
			}
		} finally {
			writer.closeFolder();
		}
		return lastExportedMessageDate;
	}

	/**
	 * Create Java objects and store them in fields. Does not physically create files.
	 */
	protected abstract boolean prepareDestinationObjects(String baseName, String extension);

	/**
	 * Usually, check if outFile exists
	 */
	protected abstract boolean shouldLoadOldestMessageToFetchFromPreferences();

	protected void checkQuota(MessagesMetaData<?> messages) {
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
				+ email.getUserName().replace('/', '\\') + '@'
				+ (email.getServerAddress().replace('/', '\\')) + '/'
				+ (email.getCurrentFolderId().replace('/', '\\'));
		if (create || prefs.nodeExists(key)) {
			return prefs.node(key);
		} else {
			return null;
		}
	}

}
