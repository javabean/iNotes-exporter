package fr.cedrik.inotes.util;

import java.text.Format;
import java.util.Locale;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * @author C&eacute;drik LIME
 */
public abstract class DateUtils {
	/**
	 * RFC 5322 datetime format: {@value}
	 * @see <a href="http://www.ietf.org/rfc/rfc5322.txt">RFC 5322</a>
	 */
	public static final String RFC2822_DATE_TIME = "EEE, d MMM yyyy HH:mm:ss Z";//$NON-NLS-1$
	public static final Format RFC2822_DATE_TIME_FORMAT = FastDateFormat.getInstance(RFC2822_DATE_TIME, Locale.US);

	/**
	 * C asctime / ctime: "Tue May 21 13:46:22 1991" "Sat Jan  3 01:05:34 1996"
	 */
	public static final String MBOX_DATE_TIME = "EEE MMM dd HH:mm:ss yyyy";//$NON-NLS-1$
	public static final Format MBOX_DATE_TIME_FORMAT = FastDateFormat.getInstance(MBOX_DATE_TIME, Locale.US);

	/**
	 * Default ISO 8601 datetime format: {@value}
	 * @see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601 DateTime</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3399</a>
	 */
	public static final String ISO8601_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ssZZ";//$NON-NLS-1$
	public static final Format ISO8601_DATE_TIME_FORMAT = FastDateFormat.getInstance(ISO8601_DATE_TIME);
	public static final String ISO8601_DATE = "yyyy-MM-dd";//$NON-NLS-1$
	public static final String ISO8601_TIME = "HH:mm:ssZ";//$NON-NLS-1$


	private DateUtils() {
		super();
	}

}
