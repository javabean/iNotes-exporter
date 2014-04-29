/**
 *
 */
package fr.cedrik.inotes.fs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.util.Charsets;

/**
 * Usage:
 * <ul><li>{@link #openFolder(boolean)}</li>
 * <li>{@link #openFile(BaseINotesMessage, boolean)}</li>
 * <li>{@link #write(BaseINotesMessage, Iterator)}</li>
 * <li>{@link #closeFile(BaseINotesMessage)} (in {@code finally} block!)</li>
 * <li>{@link #closeFolder()} (in {@code finally} block!)</li></ul>
 *
 * @author C&eacute;drik LIME
 */
public abstract class BaseFileWriter {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected File outFile = null;
	protected Writer outWriter = null;
	protected FileLock outFileLock = null;

	public BaseFileWriter() {
	}

	public abstract boolean exists();

	public abstract void openFolder(boolean append) throws IOException;

	public abstract void openFile(BaseINotesMessage message, boolean append) throws IOException;

	protected void openWriter(BaseINotesMessage message, boolean append) throws IOException {
		outFile = getMBoxFile(message);
		OutputStream outStream = new FileOutputStream(outFile, append);
		outFileLock = ((FileOutputStream)outStream).getChannel().tryLock();
		if (outFileLock == null) {
			logger.error("Can not acquire a lock on file " + outFile + ". Aborting.");
			IOUtils.closeQuietly(outStream);
			throw new IOException("Can not acquire a lock on file " + outFile + ". Aborting.");
		}
		if (canCompress() /* || iNotes.isCompressExports() */) {
			outStream = new GZIPOutputStream(outStream, 8*1024);
		}
		outWriter = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
	}

	protected abstract boolean canCompress();

	protected abstract File getMBoxFile(BaseINotesMessage message);

	public final void write(BaseINotesMessage message, Iterator<String> mime) throws IOException {
		logger.debug("Writing message {}", message);
		writeMIME(outWriter, message, mime);
	}

	public void flush() throws IOException {
		if (outWriter != null) {
			outWriter.flush();
		}
	}

	public abstract void closeFile(BaseINotesMessage message) throws IOException;

	public abstract void closeFolder() throws IOException;

	protected void closeWriter() throws IOException {
		if (outWriter != null) {
			outWriter.flush();
		}
		if (outFileLock != null) {
			outFileLock.release();
			outFileLock = null;
		}
		IOUtils.closeQuietly(outWriter);
		outWriter = null;
		// Do not set outFile to null, as it may be used by sub-classes!
	}

	protected abstract void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException;//FIXME replace BaseINotesMessage with "extraHeaders"?

	/**
	 * @return RFC-compliant new-line char
	 */
	protected String newLine() {
		return "\n";
	}
	/**
	 * @return RFC-compliant new-line char
	 */
	protected Writer newLine(Writer out) throws IOException {
		return out.append(newLine());
	}
}
