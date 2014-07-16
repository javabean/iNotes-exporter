package fr.cedrik.util;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link Charset} cache to avoid pre-Java 7 slow performance under load...
 * @author C&eacute;drik LIME
 * @see java.nio.charset.StandardCharsets (Java 7)
 */
public class Charsets {
	private static final Map<String, Charset> charsetCache = new ConcurrentHashMap<String, Charset>();

	/**
	 * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
	 * Unicode character set
	 */
	public static final Charset US_ASCII = forName("US-ASCII");//$NON-NLS-1$
	/**
	 * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
	 */
	public static final Charset ISO_8859_1 = forName("ISO-8859-1");//$NON-NLS-1$
	/**
	 * Eight-bit UCS Transformation Format
	 */
	public static final Charset UTF_8 = forName("UTF-8");//$NON-NLS-1$
	/**
	 * Sixteen-bit UCS Transformation Format, big-endian byte order
	 */
	public static final Charset UTF_16BE = forName("UTF-16BE");//$NON-NLS-1$
	/**
	 * Sixteen-bit UCS Transformation Format, little-endian byte order
	 */
	public static final Charset UTF_16LE = forName("UTF-16LE");//$NON-NLS-1$
	/**
	 * Sixteen-bit UCS Transformation Format, byte order identified by an
	 * optional byte-order mark
	 */
	public static final Charset UTF_16 = forName("UTF-16");//$NON-NLS-1$


	private Charsets() {
		assert false;
	}

	/**
	 * @see Charset#forName(String)
	 */
	public static Charset forName(String encoding) throws IllegalCharsetNameException, UnsupportedCharsetException {
		Charset charset = charsetCache.get(encoding);
		if (charset == null) {
			charset = Charset.forName(encoding);
			charsetCache.put(encoding, charset);
		}
		return charset;
	}

}
