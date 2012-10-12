/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.fs.BaseFsExport;
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
abstract class BaseMBox extends BaseFsExport implements fr.cedrik.inotes.MainRunner.Main {

	protected File outFile;

	public BaseMBox() throws IOException {
	}

	@Override
	protected boolean validateDestinationName(String baseName, String extension) {
		String fileName = baseName;
		if (! fileName.endsWith(extension)) {
			fileName += extension;
		}
		this.outFile = new File(fileName);
		return true;
	}

	@Override
	protected boolean shouldLoadOldestMessageToFetchFromPreferences() {
		return outFile.exists();
	}

	@Override
	protected final Date export(INotesMessagesMetaData<?> messages) throws IOException {
		Writer mbox = null;
		FileLock outFileLock = null;
		Date lastExportedMessageDate = null;
		try {
			// open out file
			boolean append = oldestMessageToFetch != null;
			FileOutputStream outStream = new FileOutputStream(outFile, append);
			outFileLock = outStream.getChannel().tryLock();
			if (outFileLock == null) {
				logger.error("Can not acquire a lock on file " + outFile + ". Aborting.");
				return null;
			}
			mbox = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
			// write messages
			for (BaseINotesMessage message : messages.entries) {
				IteratorChain<String> mime = session.getMessageMIME(message);
				if (mime == null || ! mime.hasNext()) {
					logger.error("Empty MIME message! ({})", message);
					break;
				}
				logger.debug("Writing message {}", message);
				try {
					writeMIME(mbox, message, mime);
				} finally {
					mime.close();
				}
				lastExportedMessageDate = message.getDate();
			}
			mbox.flush();
		} finally {
			if (outFileLock != null) {
				outFileLock.release();
			}
			IOUtils.closeQuietly(mbox);
		}
		return lastExportedMessageDate;
	}

	protected void writeFromLine(Writer mbox, BaseINotesMessage message) throws IOException {
		// date should be UTC, but tests show there is no need to convert it
		mbox.append("From MAILER-DAEMON ").append(DateUtils.MBOX_DATE_TIME_FORMAT.format(message.getDate())).append('\n');
	}

}
