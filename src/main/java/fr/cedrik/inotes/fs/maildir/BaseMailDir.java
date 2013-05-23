/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.mail.MailParseException;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.INotesMessagesMetaData;
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
		return mailDir.exists() && mailDir.list().length != 0;
	}

	@Override
	protected final Date export(INotesMessagesMetaData<? extends BaseINotesMessage> messages, boolean deleteExportedMessages) throws IOException {
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
		List<BaseINotesMessage> writtenMessages = new ArrayList<BaseINotesMessage>();
		for (BaseINotesMessage message : messages.entries) {
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
			logger.debug("Writing message {}", message);
			// open out file
			File tmpFile = new File(tmpDir, getMailFileName(message));
			OutputStream outStream = new FileOutputStream(tmpFile);
			FileLock tmpFileLock = ((FileOutputStream)outStream).getChannel().tryLock();
			if (tmpFileLock == null) {
				logger.error("Can not acquire a lock on file " + tmpFile + ". Aborting.");
				IOUtils.closeQuietly(outStream);
				IOUtils.closeQuietly(mime);
				break;
			}
			// Do not allow compression, since the filename lacks size information (,S=<size>),
//			if (iNotes.isCompressExports()) {
//				outStream = new GZIPOutputStream(outStream, 8*1024);
//			}
			Writer mbox = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
			try {
				writeMIME(mbox, message, mime);
				writtenMessages.add(message);
			} finally {
				IOUtils.closeQuietly(mime);
				mbox.flush();
				tmpFileLock.release();
				IOUtils.closeQuietly(mbox);
			}
			// the modification time of the file is the delivery date of the message.
			tmpFile.setLastModified(message.getDate().getTime());
			// move the message from tmp to new
			File newFile = new File(newDir, getMailFileName(message));
			if (! tmpFile.renameTo(newFile)) {
				logger.warn("Can not move file {} to {}", tmpFile, newFile);
			}
			lastExportedMessageDate = message.getDate();
		}
		if (deleteExportedMessages) {
			session.deleteMessage(writtenMessages);
		}
		return lastExportedMessageDate;
	}

	protected String getMailFileName(BaseINotesMessage message) {
		// time.pid.host
		// here we use use iNotes date + iNotes unid
		return "" + message.getDate().getTime() + '-' + message.unid;
	}

	@Override
	protected void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException {
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append(newLine());
		}
	}

}
