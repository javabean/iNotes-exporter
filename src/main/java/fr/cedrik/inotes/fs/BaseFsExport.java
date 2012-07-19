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

import fr.cedrik.inotes.MessageMetaData;
import fr.cedrik.inotes.MessagesMetaData;
import fr.cedrik.inotes.Session;
import fr.cedrik.inotes.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseFsExport implements fr.cedrik.inotes.MainRunner.Main {
	protected static final String ISO8601_DATE_SEMITIME = "yyyy-MM-dd'T'HH:mm";//$NON-NLS-1$

	protected static final String PREF_LAST_EXPORT_DATE = "lastExportDate";//$NON-NLS-1$

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	protected Session session;
	protected Date oldestMessageToFetch;

	public BaseFsExport() throws IOException {
	}

	protected void run(String[] args, String extension) throws IOException {
		if (args.length == 0) {
			help();
			System.exit(-1);
		}
		if (! prepareOutFileFields(args[0], extension)) {
			return;
		}
		if (args.length > 1) {
			try {
				this.oldestMessageToFetch = new SimpleDateFormat(ISO8601_DATE_SEMITIME).parse(args[1]);
			} catch (ParseException ignore) {
				logger.warn("Bad date format. Please use " + ISO8601_DATE_SEMITIME);
			}
		} else if (shouldLoadOldestMessageToFetchFromPreferences()) {
			// set oldestMessageToFetch if file exists, and there is a Preference
			Preferences prefs = Preferences.userNodeForPackage(this.getClass());
			try {
				if (prefs.nodeExists(this.getClass().getSimpleName())) {
					long lastExportDate = prefs.node(this.getClass().getSimpleName()).getLong(PREF_LAST_EXPORT_DATE, -1);
					if (lastExportDate != -1) {
						this.oldestMessageToFetch = new Date(lastExportDate);
					}
				}
			} catch (BackingStoreException ignore) {
				logger.warn("Can not load last import date:", ignore);
			}
		}
		if (this.oldestMessageToFetch != null) {
			logger.info("Incremental import from " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.oldestMessageToFetch));
		} else {
			logger.info("Full import");
		}
		session = new Session();
		// login
		if (! session.login()) {
			logger.error("Can not login!");
			return;
		}
		try {
			// meta-data
			MessagesMetaData messages = session.getMessagesMetaData(oldestMessageToFetch);
			checkQuota(messages);
			if (! messages.entries.isEmpty()) {
				this.export(messages);
				// set Preference to oldestMessageToFetch
				setPreferenceToOldestMessageToFetch(messages);
			}
		} finally {
			session.logout();
		}
	}

	protected abstract void help();

	protected abstract void export(MessagesMetaData messages) throws IOException;

	/**
	 * Create objects and store them in fields. Does not physically create files.
	 */
	protected abstract boolean prepareOutFileFields(String baseName, String extension);

	/**
	 * Usually, check if outFile exists
	 */
	protected abstract boolean shouldLoadOldestMessageToFetchFromPreferences();

	protected abstract void writeMIME(Writer mbox, MessageMetaData message, Iterator<String> mime) throws IOException;

	protected void checkQuota(MessagesMetaData messages) {
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

	protected void setPreferenceToOldestMessageToFetch(MessagesMetaData messages) {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		Date lastExportDate = messages.entries.get(messages.entries.size()-1).date;
		prefs.node(this.getClass().getSimpleName()).putLong(PREF_LAST_EXPORT_DATE, lastExportDate.getTime()+1);// +1: don't re-import last imported message next time...
		try {
			prefs.flush();
		} catch (BackingStoreException ignore) {
			logger.warn("Can not store last import date: " + DateUtils.ISO8601_DATE_TIME_FORMAT.format(lastExportDate), ignore);
		}
	}
}
