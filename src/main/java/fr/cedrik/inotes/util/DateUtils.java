package fr.cedrik.inotes.util;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author C&eacute;drik LIME
 */
public abstract class DateUtils {
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");// TimeZone.GMT_ID
	private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

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


	public static Date parseLotusXMLDate(String dateStr) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss','SS'Z'");//$NON-NLS-1$
		df.setTimeZone(UTC);
		return df.parse(dateStr);
	}

	/**
	 * fix Lotus Notes broken date pattern: 29-Oct-2012 19:23:20 CET	10-Oct-2012 11:25:11 CEDT
	 */
	public static String fixLotusMIMEDateHeader(String line) {
		if (line.startsWith("Date: ") && line.contains("-")) {//$NON-NLS-1$//$NON-NLS-2$
			final DateFormat LOTUS_NOTES_BROKEN_DATE_FORMAT = new SimpleDateFormat("d-MMM-yyyy HH:mm:ss z", Locale.US);//$NON-NLS-1$
			LOTUS_NOTES_BROKEN_DATE_FORMAT.setLenient(false);
			String lineToParse = line;
			try {
				int minutesToAdd = 0;
				// TimeZones not recognized by SimpleDateFormat
				for (Map.Entry<String, Integer> tz : LOTUS_TZ.entrySet()) {
					if (lineToParse.endsWith(tz.getKey())) {
						lineToParse = line.substring(0, line.length() - tz.getKey().length()) + ' ' + GMT_ID;
						minutesToAdd = - tz.getValue().intValue();
					}
				}
				Date date = LOTUS_NOTES_BROKEN_DATE_FORMAT.parse(lineToParse.substring("Date: ".length()).trim());//$NON-NLS-1$
				if (minutesToAdd != 0) {
					date.setTime(date.getTime() + minutesToAdd * org.apache.commons.lang3.time.DateUtils.MILLIS_PER_MINUTE);
				}
				String rfcLine = "Date: " + RFC2822_DATE_TIME_FORMAT.format(date);//$NON-NLS-1$
				logger.debug("Fixing broken Lotus Date header; before: {}\tafter: {}", line, rfcLine);
				line = rfcLine;
			} catch (ParseException notAnError) {
				logger.debug("Date header OK: {}", line);
			}
		}
		return line;
	}

	private static final String GMT_ID = "GMT";// TimeZone.GMT_ID
	private static final Map<String, Integer> LOTUS_TZ = new HashMap<String, Integer>();
	// see http://www.ibm.com/developerworks/lotus/library/ls-keeping_time/side1.html
	// see http://tools.ietf.org/html/rfc5322#section-4.3
	static {
//		LOTUS_TZ.put(" GMT", Integer.valueOf(0*60));     // Greenwich Mean Time
		LOTUS_TZ.put(" GDT", Integer.valueOf((0+1)*60));
		LOTUS_TZ.put(" ZW1", Integer.valueOf(-1*60));
		LOTUS_TZ.put(" YW1", Integer.valueOf((-1+1)*60));
		LOTUS_TZ.put(" ZW1", Integer.valueOf(-2*60));
		LOTUS_TZ.put(" YW2", Integer.valueOf((-2+1)*60));
		LOTUS_TZ.put(" ZW3", Integer.valueOf(-3*60));
		LOTUS_TZ.put(" YW3", Integer.valueOf((-3+1)*60));
		LOTUS_TZ.put(" NST", Integer.valueOf(-3*60-30)); // Newfoundland
		LOTUS_TZ.put(" NDT", Integer.valueOf((-3+1)*60-30));
		LOTUS_TZ.put(" AST", Integer.valueOf(-4*60));    // Atlantic Standard Time
		LOTUS_TZ.put(" ADT", Integer.valueOf((-4+1)*60));
		LOTUS_TZ.put(" EST", Integer.valueOf(-5*60));    // Eastern Standard Time
		LOTUS_TZ.put(" EDT", Integer.valueOf((-5+1)*60));
		LOTUS_TZ.put(" CST", Integer.valueOf(-6*60));    // Central Standard Time
		LOTUS_TZ.put(" CDT", Integer.valueOf((-6+1)*60));
		LOTUS_TZ.put(" MST", Integer.valueOf(-7*60));    // Mountain Standard Time
		LOTUS_TZ.put(" MDT", Integer.valueOf((-7+1)*60));
		LOTUS_TZ.put(" PST", Integer.valueOf(-8*60));    // Pacific Standard Time
		LOTUS_TZ.put(" PDT", Integer.valueOf((-8+1)*60));
		LOTUS_TZ.put(" YST", Integer.valueOf(-9*60));    // Alaska Standard Time
		LOTUS_TZ.put(" YDT", Integer.valueOf((-9+1)*60));
		LOTUS_TZ.put(" ZW9B",  Integer.valueOf(-9*60-30));
		LOTUS_TZ.put(" HST",   Integer.valueOf(-10*60)); // Hawaii-Aleutian Standard Time
		LOTUS_TZ.put(" HDT",   Integer.valueOf((-10+1)*60));
		LOTUS_TZ.put(" BST",   Integer.valueOf(-11*60)); // Bering Standard Time
		LOTUS_TZ.put(" BDT",   Integer.valueOf((-11+1)*60));
		LOTUS_TZ.put(" ZW12",  Integer.valueOf(-12*60));
		LOTUS_TZ.put(" ZE12C", Integer.valueOf(12*60+45));
		LOTUS_TZ.put(" ZE12",  Integer.valueOf(12*60));
		LOTUS_TZ.put(" ZE11B", Integer.valueOf(11*60+30));
		LOTUS_TZ.put(" ZE11",  Integer.valueOf(11*60));
		LOTUS_TZ.put(" ZE10B", Integer.valueOf(10*60+30));
		LOTUS_TZ.put(" ZE10",  Integer.valueOf(10*60));
		LOTUS_TZ.put(" ZE9B",  Integer.valueOf(9*60+30));
		LOTUS_TZ.put(" ZE9",   Integer.valueOf(9*60));
		LOTUS_TZ.put(" ZE8",   Integer.valueOf(8*60));
		LOTUS_TZ.put(" ZE7",   Integer.valueOf(7*60));
		LOTUS_TZ.put(" ZE6B",  Integer.valueOf(6*60+30));
		LOTUS_TZ.put(" ZE6",   Integer.valueOf(6*60));
		LOTUS_TZ.put(" ZE5C",  Integer.valueOf(5*60+45));
		LOTUS_TZ.put(" ZE5B",  Integer.valueOf(5*60+30));
		LOTUS_TZ.put(" ZE5",   Integer.valueOf(5*60));
		LOTUS_TZ.put(" ZE4B",  Integer.valueOf(4*60+30));
		LOTUS_TZ.put(" ZE4",   Integer.valueOf(4*60));
		LOTUS_TZ.put(" ZE3B",  Integer.valueOf(3*60+30));
		LOTUS_TZ.put(" ZE3",   Integer.valueOf(3*60));
		LOTUS_TZ.put(" ZE2",   Integer.valueOf(2*60));
		LOTUS_TZ.put(" CET",   Integer.valueOf(1*60));   // Central European Time
		LOTUS_TZ.put(" CEDT",  Integer.valueOf((1+1)*60));
		// remove JVM-known TZ entries
		Iterator<Map.Entry<String, Integer>> iterator = LOTUS_TZ.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Integer> lotus = iterator.next();
			TimeZone tz = TimeZone.getTimeZone(lotus.getKey().trim());
			if (tz != null && ! GMT_ID.equals(tz.getID())) {
//				logger.debug("Removing existing TZ: {}", lotus.getKey());
				iterator.remove();
			}
		}
	}
}
