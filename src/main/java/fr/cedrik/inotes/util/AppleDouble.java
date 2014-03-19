/**
 *
 */
package fr.cedrik.inotes.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.FilenameUtils;


/**
 * Utility class for writing AppleDouble (v2) files.
 *
 * @author  C&eacute;drik LIME
 * @see "http://support.apple.com/kb/TA32537"
 */
/*
Field Length
----- ------
Magic number ------- 4 bytes -- 0x00051600
Version number ------ 4 bytes -- 0x00020000
Filler ------------- 16 bytes -- all zeros (0x00)
Number of entries ----- 2 bytes -- unsigned 16-bit number

Entry descriptor for each entry:
Entry ID ------ 4 bytes -- unsigned 32-bit number, defines what the entry is. Entry IDs range from 1 to $FFFFFFFF. Entry ID 0 is invalid.
Offset -------- 4 bytes -- unsigned 32-bit number, shows the offset from the beginning of the file to the beginning of the entry's data.
Length -------- 4 bytes -- unsigned 32-bit number, shows the length of the data in bytes. The length can be 0.
 */
// NOTE: Byte ordering in the file header fields follows MC68000 conventions, most significant byte first.
// NOTE: dates are stored as number of seconds since jan 01 2000 GMT
// NOTE: The entry data follows all of the entry descriptors.
//		The data in each entry must be in a single, contiguous block.
//		You can leave holes in the file for later expansion of data. For example, even if a file's comment field is only 10 bytes long,
//		you can place the offset of the next field 200 bytes beyond the offset of the comment field, leaving room for the comment to grow to its maximum length of 200 bytes.
public class AppleDouble {

	public static final String APPLE_DOUBLE__MACOSX_FOLDER = "__MACOSX";//$NON-NLS-1$
	public static final String APPLE_DOUBLE__FILE_PREFIX = "._";//$NON-NLS-1$

	public static final long MAGIC_NUMBER = 0x00051607;
	public static final long VERSION_NUMBER = 0x00020000;
	private static final byte[] FILLER = "Mac OS X        ".getBytes(Charsets.US_ASCII); // new byte[16];

	static {
		assert FILLER.length == 16 : FILLER.length;
	}

	// TODO Factory to create different kinds of EntryDescriptors
	public static class EntryDescriptor {
		/** Data fork */
		public static final short DATA_FORK = 1;
		/** Resource fork */
		public static final short RESOURCE_FORK = 2;
		/** File's name as created on home file system */
		public static final short REAL_NAME = 3;
		/** Standard Macintosh comment */
		public static final short COMMENT = 4;
		/** Standard Macintosh black and white icon */
		public static final short ICON_BW = 5;
		/** Macintosh color icon */
		public static final short ICON_COLOR = 6;
		/**
		 * File creation date, modification date, and so on
		 *
		 * The File Dates Info entry (ID=8) consists of the file creation, modification, backup and access times (see Figure 2-1),
		 * stored as a signed number of seconds before or after 12:00 a.m. (midnight), January 1, 2000 Greenwich Mean Time (GMT).
		 * In other words, the start of the year 2000 GMT corresponds to a date-time of 0.
		 * Applications must convert to their native date and time conventions.
		 * When initially created, a file's backup time and any unknown entries are set to $80000000 or 0x80000000, the earliest reasonable time.
		 */
		public static final short FILE_DATES_INFO = 8;
		/**
		 * Standard Macintosh Finder information
		 *
		 * The Macintosh Finder Info entry (ID=9) consists of 16 bytes of Finder Info followed by 16 bytes of extended Finder Info;
		 * that is, the field ioFlFndrInfo followed by ioFlXFndrInfo, as returned by the Macintosh PBGetCatInfo call.
		 * (The PBGetCatInfo and the internal structures of ioFlFndrInfo and ioFlXFndrInfo are described in Inside Macintosh.)
		 *
		 * Newly created files have 0s in all Finder Info subfields.
		 * If you are creating an AppleSingle or AppleDouble file, you may assign 0 to any subfield whose value is unknown
		 * (most subfields are undefined if the file does not reside on a valid hierarchical file system [HFS] volume),
		 * but you may want to set the fdType and fdCreator subfields.
		 *
		 * See http://support.apple.com/kb/TA32537 for more information.
		 */
		public static final short FINDER_INFO = 9;
		/**
		 * Macintosh file information, attributes, and so on
		 *
		 * The Macintosh File Info entry (ID=10) is 32 bits that stores the locked and protected bit.
		 * Macintosh file times are stored in entry ID 8.
		 */
		public static final short MACINTOSH_FILE_INFO = 10;
		/**
		 * ProDOS file information, attributes, and so on
		 *
		 * The ProDOS File Info entry (ID=11) consists of the file access, file type, and file auxiliary type.
		 * ProDOS file times are stored in entry ID 8.
		 */
		public static final short PRODOS_FILE_INFO = 11;
		/**
		 * MS-DOS file information, attributes, and so on
		 *
		 * The MS-DOS File Info entry (ID=12) is 16 bits that stores the MS-DOS attributes.
		 * MS-DOS file times are stored in entry ID 8.
		 */
		public static final short MSDOS_FILE_INFO = 12;
		/** AFP short name */
		public static final short SHORT_NAME = 13;
		/** AFP file information, attributes, and so on */
		public static final short AFP_FILE_INFO = 14;
		/** AFP directory ID */
		public static final short DIRECTORY_ID = 15;

		/**
		 * Apple reserves the range of entry IDs from 1 to $7FFFFFFF.
		 * The rest of the range is available for applications to define their own entries.
		 * Apple does not arbitrate the use of the rest of the range.
		 */
		private int entryID;
		private int offset;
//		private int length;
		private byte[] data;

		public EntryDescriptor(int entryID, byte[] data) {
			this.entryID = entryID;
			this.data = data;
		}

		public int getEntryID() {
			return entryID;
		}
		public void setEntryID(int entryID) {
			this.entryID = entryID;
		}

		public int getOffset() {
			return offset;
		}
		public void setOffset(int offset) {
			this.offset = offset;
		}

		public int getLength() {
			return data.length;
		}

		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
	}


	private EntryDescriptor[] entries;

	/**
	 *
	 */
	public AppleDouble(EntryDescriptor[] entries) {
		this.entries = entries;
	}


	public EntryDescriptor[] getEntries() {
		return entries;
	}
	public void setEntries(EntryDescriptor[] entries) {
		this.entries = entries;
	}

	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			int dataOffset = 0;
			writeInt(out, MAGIC_NUMBER);
			dataOffset += 4;
			writeInt(out, VERSION_NUMBER);
			dataOffset += 4;
			out.write(FILLER);
			dataOffset += FILLER.length;
			writeShort(out, entries.length);
			dataOffset += 2;
			dataOffset += entries.length * (4+4+4);
			for (EntryDescriptor entry : entries) {
				entry.setOffset(dataOffset);
				dataOffset += entry.getLength();
				writeInt(out, entry.getEntryID());
				writeInt(out, entry.getOffset());
				writeInt(out, entry.getLength());
			}
			for (EntryDescriptor entry : entries) {
				out.write(entry.getData());
			}
		} catch (IOException impossible) {
			throw new AssertionError(impossible);
		}
		return out.toByteArray();
	}

	/**
	 * Converts from AppleFile time to Unix time.
	 *
	 * An Apple file time is defined to be the signed number of seconds before or after 1 Jan 2000 00:00:00 UTC.
	 * The value 0 represents 1 Jan 2000 00:00:00 UTC;
	 * a negative time represents the number of seconds <em>until</em> 1 Jan 2000 00:00:00 UTC;
	 * and a positive value represents the number of seconds <em>after</em> 1 Jan 2000 00:00:00 UTC.
	 * A Unix time is the number of seconds since 1 Jan 1970 00:00:00 UTC.
	 *
	 * @param afTime  an AppleFile time
	 * @returna UNIX time
	 */
	public static int appleFileTimeToUnixTime(int afTime) {
		//TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * Converts from Unix time to AppleFile time.
	 *
	 * An Apple file time is defined to be the signed number of seconds before or after 1 Jan 2000 00:00:00 UTC.
	 * The value 0 represents 1 Jan 2000 00:00:00 UTC;
	 * a negative time represents the number of seconds <em>until</em> 1 Jan 2000 00:00:00 UTC;
	 * and a positive value represents the number of seconds <em>after</em> 1 Jan 2000 00:00:00 UTC.
	 * A Unix time is the number of seconds since 1 Jan 1970 00:00:00 UTC.
	 *
	 * @param unixTime  a UNIX time
	 * @return an AppleFile time
	 */
	public static int unixTimeToAppleFileTime(int unixTime) {
		//TODO
		throw new UnsupportedOperationException();
	}


	public static String getAppleDoubleFileName(String file) {
		String fullPath = FilenameUtils.getFullPath(file);
		StringBuilder result = new StringBuilder(APPLE_DOUBLE__MACOSX_FOLDER.length() + APPLE_DOUBLE__FILE_PREFIX.length() + file.length() + 1);
		result.append(APPLE_DOUBLE__MACOSX_FOLDER);
		if (! fullPath.startsWith("/")) {
			result.append('/');
		}
		result.append(fullPath);
		if (! fullPath.endsWith("/") && ! "".equals(fullPath)) {
			result.append('/');
		}
		result.append(APPLE_DOUBLE__FILE_PREFIX);
		result.append(FilenameUtils.getName(file));
		return result.toString();
	}


	private void writeLong(OutputStream out, long value) throws IOException {
		out.write((byte) (value >> 56));
		out.write((byte) (value >> 48));
		out.write((byte) (value >> 40));
		out.write((byte) (value >> 32));
		out.write((byte) (value >> 24));
		out.write((byte) (value >> 16));
		out.write((byte) (value >> 8));
		out.write((byte) value);
	}

	private void writeInt(OutputStream out, long value) throws IOException {
		out.write((byte) (value >> 24));
		out.write((byte) (value >> 16));
		out.write((byte) (value >> 8));
		out.write((byte) value);
	}

	private void writeShort(OutputStream out, int value) throws IOException {
		out.write((byte) (value >> 8));
		out.write((byte) value);
	}
}
