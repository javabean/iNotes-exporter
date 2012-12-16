/**
 *
 */
package fr.cedrik.inotes;

import java.util.Date;

import fr.cedrik.inotes.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
public class MessageMetaData extends BaseINotesMessage {
	public boolean unread = false;
	public int type = -1;//$86
	public int importance = -1;//$Importance
	// availability;//SametimeInfo
	public String from93;//$93
	public String from98;//$98
	// thread;//$ThreadColumn
	public String subject;//$73
	public Date date = new Date(0);//$70
	public int size = -1;//$106
	public int recipient = -1;//$ToStuff
	public int attachement = -1;//$97
	public int answerFlag = -1;//$109
	// userData;//$UserData

	public MessageMetaData() {
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + '[' + "unid:" + this.unid
				+ ", date:" + (this.date != null ? DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.date) : String.valueOf(this.date))
				+ ", size:" + this.size
				+ ", unread: " + this.unread + ']';
	}
}
