/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.MailParseException;

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
	private static final String EXTENSION_GZ = ".gz";//$NON-NLS-1$

	protected File outFile;

	public BaseMBox() throws IOException {
	}

	@Override
	protected void help() {
		System.out.println("Usage: "+this.getClass().getSimpleName()+" <out_file> [oldest message to fetch date: " + ISO8601_DATE_SEMITIME + " [newest message to fetch date: " + ISO8601_DATE_SEMITIME + " [--delete]]]");
	}

	@Override
	protected boolean validateDestinationName(String baseName, String extension) {
		String fileName;
		boolean compress = StringUtils.endsWithIgnoreCase(baseName, EXTENSION_GZ);
		if (compress) {
			// remove ".gz" extension
			fileName = FilenameUtils.removeExtension(baseName);
		} else {
			fileName = baseName;
		}
		if (! fileName.endsWith(extension)) {
			fileName += extension;
		}
		if (compress) {
			fileName += EXTENSION_GZ;
		}
		this.outFile = new File(fileName);
		return true;
	}

	@Override
	protected boolean shouldLoadOldestMessageToFetchFromPreferences() {
		return outFile.exists();
	}

	@Override
	protected final Date export(INotesMessagesMetaData<? extends BaseINotesMessage> messages, boolean deleteExportedMessages) throws IOException {
		Writer mbox = null;
		FileLock outFileLock = null;
		Date lastExportedMessageDate = null;
		try {
			// open out file
			final boolean append = outFile.exists() && (oldestMessageToFetch != null);
			OutputStream outStream = new FileOutputStream(outFile, append);
			outFileLock = ((FileOutputStream)outStream).getChannel().tryLock();
			if (outFileLock == null) {
				logger.error("Can not acquire a lock on file " + outFile + ". Aborting.");
				IOUtils.closeQuietly(outStream);
				return null;
			}
			if (/*iNotes.isCompressExports() || */StringUtils.endsWithIgnoreCase(outFile.getName(), EXTENSION_GZ)) {
				if (append) {
					logger.error("Can not append data to a compressed file ({})! Aborting.", outFile.getName());
					IOUtils.closeQuietly(outStream);
					return null;
				} else {
					outStream = new GZIPOutputStream(outStream, 8*1024);
//					iNotes.setCompressExports(true);
				}
			}
			mbox = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
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
				try {
					writeMIME(mbox, message, mime);
					writtenMessages.add(message);
				} finally {
					IOUtils.closeQuietly(mime);
				}
				if (message.getDate().getTime() > 0) {
					lastExportedMessageDate = message.getDate();
				}
			}
			mbox.flush();
			if (deleteExportedMessages) {
				session.deleteMessage(writtenMessages);
			}
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
