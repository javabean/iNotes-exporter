/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.annotation.PreDestroy;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.fs.BaseFileWriter;

/**
 * @author C&eacute;drik LIME
 */
public class MHWriter extends BaseFileWriter {

	protected File mailDir;

	public MHWriter(File mailDir) throws IOException {
		if ((mailDir.exists() && ! mailDir.isDirectory()) || (! mailDir.exists() && ! mailDir.mkdirs())) {
			logger.error("Not a directory, or can not create directory: " + mailDir);
			throw new IOException("Not a directory, or can not create directory: " + mailDir);
		}
		this.mailDir = mailDir;
	}

	@Override
	public void openFolder(boolean append) throws IOException {
		// noop
	}

	@Override
	public void openFile(BaseINotesMessage message, boolean append) throws IOException {
		openWriter(message, append);
	}

	@Override
	public boolean exists() {
		return mailDir.exists() && mailDir.list().length != 0;
	}

	@Override
	protected boolean canCompress() {
		// TODO Does MH support compressed email files?
		return false;
	}

	@Override
	protected File getMBoxFile(BaseINotesMessage message) {
		return new File(mailDir, getMailFileName(message));
	}

	@Override
	@PreDestroy
	public void closeFile(BaseINotesMessage message) throws IOException {
		closeWriter();
		// the modification time of the file is the delivery date of the message.
		outFile.setLastModified(message.getDate().getTime());
	}

	@Override
	public void closeFolder() {
		// noop
	}

	@Override
	protected void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException {
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append(newLine());
		}
	}

	protected String getMailFileName(BaseINotesMessage message) {
		long id = message.getDate().getTime();
		while (new File(mailDir, String.valueOf(id)).exists()) {
			++id;
		}
		return String.valueOf(id);
	}
}
