/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.io.output.FileWriterWithEncoding;
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
	protected Writer mbox;
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
		} else if (outFile.exists() && outFile.canWrite()) {
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
		if (! session.login()) {
			logger.error("Can not login!");
			return;
		}
		MessagesMetaData messages = session.getMessagesMetaData(oldestMessageToFetch);
		openOutputFile();
		if (! messages.entries.isEmpty()) {
			for (MessageMetaData message : messages.entries) {
				IteratorChain<String> mime = session.getMessageMIME(message);
				if (! mime.hasNext()) {
					logger.warn("Empty MIME message! ({})", message.date);
					continue;
				}
				logger.debug("Writing message {}", message);
				writeMIME(message, mime);
				mime.close();
			}
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
		mbox.close();
		session.logout();
	}

	protected void openOutputFile() throws IOException {
		boolean append = oldestMessageToFetch != null;
		mbox = new BufferedWriter(new FileWriterWithEncoding(outFile, Charsets.US_ASCII, append), 32*1024);
	}

	protected abstract void writeMIME(MessageMetaData message, Iterator<String> mime) throws IOException;

	protected void writeFromLine(MessageMetaData message) throws IOException {
		// date should be UTC, but tests show there is no need to convert it
		mbox.append("From MAILER-DAEMON ").append(DateUtils.MBOX_DATE_TIME_FORMAT.format(message.date)).append('\n');
	}

}
