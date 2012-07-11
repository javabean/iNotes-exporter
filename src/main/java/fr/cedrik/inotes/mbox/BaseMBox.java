/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.Format;
import java.util.Locale;

import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.inotes.MessageMetaData;
import fr.cedrik.inotes.MessagesMetaData;
import fr.cedrik.inotes.Session;

/**
 * @see "http://en.wikipedia.org/wiki/Mbox"
 * @see "http://tools.ietf.org/html/rfc4155"
 * @see "http://www.qmail.org/man/man5/mbox.html"
 * @see "http://homepage.ntlworld.com./jonathan.deboynepollard/FGA/mail-mbox-formats.html"
 *
 * @author C&eacute;drik LIME
 */
abstract class BaseMBox {
	/**
	 * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
	 * Unicode character set
	 */
	private static final Charset US_ASCII = Charset.forName("US-ASCII");//TODO Java 7: replace with StandardCharsets.US_ASCII

	/**
	 * RFC 5322 datetime format: {@value}
	 * @see <a href="http://www.ietf.org/rfc/rfc5322.txt">RFC 5322</a>
	 */
	protected static final Format RFC2822_DATE_TIME_FORMAT = FastDateFormat.getInstance("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);//$NON-NLS-1$

	/**
	 * Default ISO 8601 datetime format: {@value}
	 * @see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601 DateTime</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3399</a>
	 */
	protected static final Format ISO8601_DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZZ");//$NON-NLS-1$

	/**
	 * C asctime / ctime: "Tue May 21 13:46:22 1991" "Sat Jan  3 01:05:34 1996"
	 */
	protected static final Format MBOX_DATE_TIME_FORMAT = FastDateFormat.getInstance("EEE MMM dd HH:mm:ss yyyy", Locale.US);//$NON-NLS-1$

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	protected Session session;
	protected Writer mbox;

	public BaseMBox(File out) throws IOException {
		session = new Session();
		mbox = new BufferedWriter(new FileWriterWithEncoding(out, US_ASCII), 32*1024);
	}

	protected final void run() throws IOException {
		if (! session.login()) {
			logger.error("Can not login!");
			return;
		}
		MessagesMetaData messages = session.getMessagesMetaData();
		if (! messages.entries.isEmpty()) {
			for (MessageMetaData message : messages.entries) {
				LineIterator mime = session.getMessageMIME(message);
				if (! mime.hasNext()) {
					logger.warn("Empty MIME message! ({})", message.date);
					continue;
				}
				logger.debug("Writing message {}", message);
				writeMIME(message, mime);
				mime.close();
			}
		}
		mbox.close();
		session.logout();
	}

	protected abstract void writeMIME(MessageMetaData message, LineIterator mime) throws IOException;

	protected void writeFromLine(MessageMetaData message) throws IOException {
		// FIXME date should be UTC; need to convert it!
		mbox.append("From MAILER-DAEMON ").append(MBOX_DATE_TIME_FORMAT.format(message.date)).append('\n');
	}
}
