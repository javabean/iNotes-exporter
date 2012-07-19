/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.inotes.MessageMetaData;
import fr.cedrik.inotes.MessagesMetaData;
import fr.cedrik.inotes.Session;
import fr.cedrik.inotes.util.Charsets;
import fr.cedrik.inotes.util.DateUtils;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @see "http://en.wikipedia.org/wiki/Mbox"
 * @see "http://tools.ietf.org/html/rfc4155"
 * @see "http://www.qmail.org/man/man5/mbox.html"
 * @see "http://homepage.ntlworld.com./jonathan.deboynepollard/FGA/mail-mbox-formats.html"
 *
 * @author C&eacute;drik LIME
 */
abstract class BaseMBox implements fr.cedrik.inotes.MainRunner.Main {
	protected static final String ISO8601_DATE_SEMITIME = "yyyy-MM-dd'T'HH:mm";//$NON-NLS-1$

	protected static final String PREF_LAST_EXPORT_DATE = "lastExportDate";//$NON-NLS-1$

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	protected Session session;
	protected File outFile;
	protected Date oldestMessageToFetch;

	public BaseMBox() throws IOException {
	}

	protected void run(String[] args, String extension) throws IOException {
		if (args.length == 0) {
			help();
			System.exit(-1);
		}
		String fileName = args[0];
		if (! fileName.endsWith(extension)) {
			fileName += extension;
		}
		this.outFile = new File(fileName);
		if (args.length > 1) {
			try {
				this.oldestMessageToFetch = new SimpleDateFormat(ISO8601_DATE_SEMITIME).parse(args[1]);
			} catch (ParseException ignore) {
				logger.warn("Bad date format. Please use " + ISO8601_DATE_SEMITIME);
			}
		} else if (outFile.exists()) {
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
		this.export();
	}

	protected abstract void help();

	protected final void export() throws IOException {
		// login
		if (! session.login()) {
			logger.error("Can not login!");
			return;
		}
		// meta-data
		MessagesMetaData messages = session.getMessagesMetaData(oldestMessageToFetch);
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
		Writer mbox = null;
		FileLock outFileLock = null;
		try {
			if (! messages.entries.isEmpty()) {
				// open out file
				boolean append = oldestMessageToFetch != null;
				FileOutputStream outStream = new FileOutputStream(outFile, append);
				outFileLock = outStream.getChannel().tryLock();
				if (outFileLock == null) {
					logger.error("Can not acquire a lock on file " + outFile.getPath() + ". Aborting.");
					return;
				}
				mbox = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
				// write messages
				for (MessageMetaData message : messages.entries) {
					IteratorChain<String> mime = session.getMessageMIME(message);
					if (! mime.hasNext()) {
						logger.warn("Empty MIME message! ({})", message);
						continue;
					}
					logger.debug("Writing message {}", message);
					writeMIME(mbox, message, mime);
					mime.close();
				}
				mbox.flush();
				// set Preference to oldestMessageToFetch
				{
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
		} finally {
			if (outFileLock != null) {
				outFileLock.release();
			}
			IOUtils.closeQuietly(mbox);
			session.logout();
		}
	}

	protected abstract void writeMIME(Writer mbox, MessageMetaData message, Iterator<String> mime) throws IOException;

	protected void writeFromLine(Writer mbox, MessageMetaData message) throws IOException {
		// date should be UTC, but tests show there is no need to convert it
		mbox.append("From MAILER-DAEMON ").append(DateUtils.MBOX_DATE_TIME_FORMAT.format(message.date)).append('\n');
	}

}
