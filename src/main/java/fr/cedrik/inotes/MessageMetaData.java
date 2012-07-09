/**
 *
 */
package fr.cedrik.inotes;

import java.text.Format;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * @author C&eacute;drik LIME
 */
public class MessageMetaData {
	/**
	 * Default ISO 8601 datetime format: {@value}
	 * @see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601 DateTime</a>
	 * @see <a href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3399</a>
	 */
	private static final Format ISO8601_DATE_TIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZZ");//$NON-NLS-1$

	public String unid;
	public String noteid;
	public boolean unread = false;
	public int type = -1;//$86
	public int importance = -1;//$Importance
	// availability;//SametimeInfo
	public String from93;//$93
	public String from98;//$98
	// thread;//$ThreadColumn
	public String subject;//$73
	public Date date;//$70
	public int size = -1;//$106
	public int recipient = -1;//$ToStuff
	public int attachement = -1;//$97
	public int answerFlag = -1;//$109
	// userData;//$UserData

	public MessageMetaData() {
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + '[' + "unid:" + this.unid
				+ ", date:" + ISO8601_DATE_TIME_FORMAT.format(this.date)
				+ ", size:" + this.size
				+ ", unread: " + this.unread + ']';
	}
}
