/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.fs.BaseZipWriter;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

/**
 * @author C&eacute;drik LIME
 */
public class EMLZipWriter extends BaseZipWriter {
	public static final String MAILBOX_SUFFIX = fr.cedrik.inotes.fs.maildir.EMLWriter.EXTENSION_EML;

	// names of already-exported emails, to avoid overwriting
	protected TLongSet exportedMails = new TLongHashSet(1000); // should be per folder

	public EMLZipWriter(ZipOutputStream out, String baseName) {
		super(out, baseName);
	}

	@Override
	protected void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException {
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append(newLine());
		}
	}

	@Override
	public void openFolder(String folderChainStr) throws IOException {
		// noop
	}

	@Override
	public void openFile(String folderChainStr, BaseINotesMessage message) throws IOException {
		newZipEntry(folderChainStr, message);
	}

	@Override
	protected String computeZipMailFileName(String folderChainStr, BaseINotesMessage message) {
		String zipFolder = baseName + folderChainStr;
		long id = message.getDate().getTime();
		while (exportedMails.contains(id)) {
			++id;
		}
		String zipFQN = zipFolder + '/' + id + EMLZipWriter.MAILBOX_SUFFIX;
		exportedMails.add(id);
		return zipFQN;
	}

	@Override
	public void closeFile(BaseINotesMessage message) throws IOException {
		closeZipEntry();
	}

	@Override
	public void closeFolder() throws IOException {
		// noop
	}

	/**
	 * @return Outlook-compliant new-line char
	 */
	@Override
	protected String newLine() {
		return "\r\n";//$NON-NLS-1$
	}
}
