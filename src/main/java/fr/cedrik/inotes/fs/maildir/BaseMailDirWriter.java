/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.fs.BaseFileWriter;

/**
 * @author C&eacute;drik LIME
 */
public class BaseMailDirWriter extends BaseFileWriter {
	private static final String TMP = "tmp";//$NON-NLS-1$
	private static final String NEW = "new";//$NON-NLS-1$
	private static final String CUR = "cur";//$NON-NLS-1$

	protected File mailDir;

	protected File tmpDir;
	protected File newDir;

	public BaseMailDirWriter(File mailDir) throws IOException {
		if ((mailDir.exists() && ! mailDir.isDirectory()) || (! mailDir.exists() && ! mailDir.mkdirs())) {
			logger.error("Not a directory, or can not create directory: " + mailDir);
			throw new IOException("Not a directory, or can not create directory: " + mailDir);
		}
		tmpDir = new File(mailDir, TMP);
		if (! tmpDir.exists() && ! tmpDir.mkdirs()) {
			logger.error("Can not create directory: " + tmpDir);
			throw new IOException("Can not create directory: " + tmpDir);
		}
		newDir = new File(mailDir, NEW);
		if (! newDir.exists() && ! newDir.mkdirs()) {
			logger.error("Can not create directory: " + newDir);
			throw new IOException("Can not create directory: " + newDir);
		}
		new File(mailDir, CUR).mkdirs();
		this.mailDir = mailDir;
	}

	@Override
	public void openFolder(boolean append) throws IOException {
		// noop
	}

	@Override
	public void openFile(BaseINotesMessage message, boolean append) throws IOException {
		openWriter(message, true);
	}

	@Override
	public boolean exists() {
		return newDir.exists() && newDir.list().length != 0;
	}

	@Override
	protected boolean canCompress() {
		// Do not allow compression, since the filename lacks size information (,S=<size>),
		return false;
	}

	@Override
	protected File getMBoxFile(BaseINotesMessage message) {
		return new File(tmpDir, getMailFileName(message));
	}

	@Override
	public void closeFile(BaseINotesMessage message) throws IOException {
		closeWriter();
		// the modification time of the file is the delivery date of the message.
		outFile.setLastModified(message.getDate().getTime());
		// move the message from tmp to new
		File newFile = new File(newDir, getMailFileName(message));
		if (! outFile.renameTo(newFile)) {
			logger.warn("Can not move file {} to {}", outFile, newFile);
		}
	}

	@Override
	public void closeFolder() {
		// noop
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
