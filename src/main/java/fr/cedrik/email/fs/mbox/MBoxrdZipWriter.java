/**
 *
 */
package fr.cedrik.email.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.ArrayUtils;

import fr.cedrik.email.fs.BaseZipWriter;
import fr.cedrik.email.spi.Message;
import fr.cedrik.util.AppleDouble;
import fr.cedrik.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrdZipWriter extends BaseZipWriter {
	public static final String MAILBOX_SUFFIX = ".mbox";//fr.cedrik.inotes.fs.mbox.MBoxrd.EXTENSION_MBOXRD;//$NON-NLS-1$

	protected long latestExportedMessageDate = -1; // per folder

	public MBoxrdZipWriter(ZipOutputStream out, String baseName) {
		super(out, baseName);
	}

	@Override
	public void openFolder(String folderChainStr) throws IOException {
		newZipEntry(folderChainStr, null);
	}

	@Override
	public void openFile(String folderChainStr, Message message) throws IOException {
		// noop
	}

	@Override
	protected String computeZipMailFileName(String folderChainStr, Message message) {
		return baseName + folderChainStr + MAILBOX_SUFFIX;
	}

	@Override
	protected void writeMIME(Writer mbox, Message message, Iterator<String> mime) throws IOException {
		writeFromLine(mbox, message);
		while (mime.hasNext()) {
			String line = mime.next();
			Matcher from_ = FROM_.matcher(line);
			if (from_.find()) {
				logger.trace("Escaping {}", from_.group());
				mbox.write('>');
			}
			mbox.append(line).append(newLine());
		}
		mbox.write(newLine());
		if (message != null && message.getDate() != null) {
			latestExportedMessageDate = Math.max(latestExportedMessageDate, message.getDate().getTime());
		}
	}

	@Override
	public void closeFile(Message message) throws IOException {
		// noop
	}

	@Override
	public void closeFolder() throws IOException {
		if (latestExportedMessageDate > 0) {
			currentEntry.setTime(latestExportedMessageDate);
		}
		ZipEntry entry = currentEntry; // necessary since currentEntry is null'ed in close()
		super.closeZipEntry();
		setTEXTtype(outStream, entry);
	}


	private static final Pattern FROM_ = Pattern.compile("^>*From ");//$NON-NLS-1$

	protected void writeFromLine(Writer mbox, Message message) throws IOException {
		// date should be UTC, but tests show there is no need to convert it
		mbox.append("From MAILER-DAEMON ").append(DateUtils.MBOX_DATE_TIME_FORMAT.format(message.getDate())).append(newLine());
	}

	/**
	 * @return Outlook-compliant new-line char
	 */
	@Override
	protected String newLine() {
		return "\n";//$NON-NLS-1$
	}

	/**
	 * Outlook 2011 is an old Carbon app that requires its data file to have a 'TEXT' file type; creator is not used.
	 * @param outZip
	 * @param name
	 * @throws IOException
	 */
	protected void setTEXTtype(ZipOutputStream outZip, ZipEntry entry) throws IOException {
		ZipEntry adEntry = new ZipEntry(AppleDouble.getAppleDoubleFileName(entry.getName()));
		adEntry.setTime(entry.getTime());
		outZip.putNextEntry(adEntry);
		outZip.write(APPLE_DOUBLE__TEXT_TYPE);
		outZip.closeEntry();
	}

	// public for JUnit tests only
	public static final byte[] APPLE_DOUBLE__TEXT_TYPE;
	static {
		byte[] RESOURCE_FORK_EMPTY = ArrayUtils.EMPTY_BYTE_ARRAY;
		byte[] FINDER_INFO_TEXT = new byte[32];
		FINDER_INFO_TEXT[0] = 'T';
		FINDER_INFO_TEXT[1] = 'E';
		FINDER_INFO_TEXT[2] = 'X';
		FINDER_INFO_TEXT[3] = 'T';

		AppleDouble.EntryDescriptor[] adEntryDescriptors = new AppleDouble.EntryDescriptor[] {
				new AppleDouble.EntryDescriptor(AppleDouble.EntryDescriptor.FINDER_INFO, FINDER_INFO_TEXT),
				new AppleDouble.EntryDescriptor(AppleDouble.EntryDescriptor.RESOURCE_FORK, RESOURCE_FORK_EMPTY)
		};
		APPLE_DOUBLE__TEXT_TYPE = new AppleDouble(adEntryDescriptors).getBytes();
	}
}
