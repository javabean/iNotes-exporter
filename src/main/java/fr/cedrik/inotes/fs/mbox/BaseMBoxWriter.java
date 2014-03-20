/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.fs.BaseFileWriter;
import fr.cedrik.inotes.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseMBoxWriter extends BaseFileWriter {
	private static final String EXTENSION_GZ = ".gz";//$NON-NLS-1$

	protected String outFileName;

	public BaseMBoxWriter(String baseName, String extension) {
		super();
		// ensure file name ends with required extension
		String fileName;
		boolean compress = StringUtils.endsWithIgnoreCase(baseName, EXTENSION_GZ);
		if (compress) {
			// remove ".gz" extension
			fileName = FilenameUtils.removeExtension(baseName);
		} else {
			fileName = baseName;
		}
		if (extension != null && ! fileName.endsWith(extension)) {
			fileName += extension;
		}
		if (compress) {
			fileName += EXTENSION_GZ;
		}
		outFileName = fileName;
	}

	@Override
	public boolean exists() {
		return getMBoxFile(null).exists();
	}

	@Override
	public void openFolder(boolean append) throws FileNotFoundException, IOException {
		if (append && canCompress()) {
			logger.error("Can not append data to a compressed file ({})! Aborting.", outFile.getName());
			throw new IOException("Can not append data to a compressed file ("+outFile.getName()+")! Aborting.");
		}
		openWriter(null, append);
	}

	@Override
	public void openFile(BaseINotesMessage message, boolean append) throws IOException {
		// noop
	}

	@Override
	protected boolean canCompress() {
		return StringUtils.endsWithIgnoreCase(outFileName, EXTENSION_GZ);
	}

	@Override
	protected File getMBoxFile(BaseINotesMessage message) {
		return new File(outFileName);
	}

	@Override
	public void closeFile(BaseINotesMessage message) throws IOException {
		// noop
	}

	@Override
	public void closeFolder() throws IOException {
		closeWriter();
	}

	protected void writeFromLine(Writer mbox, BaseINotesMessage message) throws IOException {
		// date should be UTC, but tests show there is no need to convert it
		mbox.append("From MAILER-DAEMON ").append(DateUtils.MBOX_DATE_TIME_FORMAT.format(message.getDate())).append(newLine());
	}
}
