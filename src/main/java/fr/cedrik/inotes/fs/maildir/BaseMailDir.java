/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

import fr.cedrik.inotes.MessageMetaData;
import fr.cedrik.inotes.MessagesMetaData;
import fr.cedrik.inotes.fs.BaseFsExport;
import fr.cedrik.inotes.util.Charsets;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @see "http://en.wikipedia.org/wiki/Maildir"
 * @see "http://www.qmail.org/man/man5/maildir.html"
 * @see "http://cr.yp.to/proto/maildir.html"
 *
 * @author C&eacute;drik LIME
 */
abstract class BaseMailDir extends BaseFsExport implements fr.cedrik.inotes.MainRunner.Main {
	private static final String TMP = "tmp";//$NON-NLS-1$
	private static final String NEW = "new";//$NON-NLS-1$
	private static final String CUR = "cur";//$NON-NLS-1$

	protected File mailDir;

	public BaseMailDir() throws IOException {
	}

	@Override
	protected boolean validateDestinationName(String baseName, String extension) {
		String dirName = baseName;
		this.mailDir = new File(dirName);
		if ((this.mailDir.exists() && ! this.mailDir.isDirectory()) || (! this.mailDir.exists() && ! this.mailDir.mkdirs())) {
			logger.error("Not a directory, or can not create directory: " + mailDir);
			return false;
		}
		return true;
	}

	@Override
	protected boolean shouldLoadOldestMessageToFetchFromPreferences() {
		return mailDir.exists() && mailDir.list().length == 0;
	}

	@Override
	protected final Date export(MessagesMetaData messages) throws IOException {
		File tmpDir = new File(mailDir, TMP);
		if (! tmpDir.exists() && ! tmpDir.mkdirs()) {
			logger.error("Can not create directory: " + tmpDir);
			return null;
		}
		File newDir = new File(mailDir, NEW);
		if (! newDir.exists() && ! newDir.mkdirs()) {
			logger.error("Can not create directory: " + newDir);
			return null;
		}
		new File(mailDir, CUR).mkdirs();
		Date lastExportedMessageDate = null;
		// write messages
		for (MessageMetaData message : messages.entries) {
			IteratorChain<String> mime = session.getMessageMIME(message);
			if (mime == null || ! mime.hasNext()) {
				logger.error("Empty MIME message! ({})", message);
				break;
			}
			logger.debug("Writing message {}", message);
			// open out file
			File tmpFile = new File(tmpDir, getMailFileName(message));
			FileOutputStream outStream = new FileOutputStream(tmpFile);
			FileLock tmpFileLock = outStream.getChannel().tryLock();
			if (tmpFileLock == null) {
				logger.error("Can not acquire a lock on file " + tmpFile + ". Aborting.");
				break;
			}
			Writer mbox = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
			try {
				writeMIME(mbox, message, mime);
			} finally {
				mime.close();
				mbox.flush();
				tmpFileLock.release();
				IOUtils.closeQuietly(mbox);
			}
			// the modification time of the file is the delivery date of the message.
			tmpFile.setLastModified(message.date.getTime());
			// move the message from tmp to new
			File newFile = new File(newDir, getMailFileName(message));
			if (! tmpFile.renameTo(newFile)) {
				logger.warn("Can not move file {} to {}", tmpFile, newFile);
			}
			lastExportedMessageDate = message.date;
		}
		return lastExportedMessageDate;
	}

	protected String getMailFileName(MessageMetaData message) {
		// time.pid.host
		// here we use use iNotes date + iNotes unid
		return "" + message.date.getTime() + '-' + message.unid;
	}

	@Override
	protected void writeMIME(Writer mbox, MessageMetaData message, Iterator<String> mime) throws IOException {
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append('\n');
		}
	}

}
