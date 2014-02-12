/**
 *
 */
package fr.cedrik.inotes;

import java.util.Date;

import fr.cedrik.inotes.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
public class MeetingNoticeMetaData extends BaseINotesMessage {
	//public List<Integer> xxx;//$149
	public Date meetingDate;//$144
	// availability;//SametimeInfo
	public String from;//$2
	public Date date = new Date(0);//$3
	public String subject;//$147
	//public Date xxx;//$146
	//public int xxx;//$154

	public MeetingNoticeMetaData() {
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + '[' + "unid:" + this.unid
				+ ", meetingDate:" + (this.meetingDate != null ? DateUtils.ISO8601_DATE_TIME_FORMAT.format(this.meetingDate) : String.valueOf(this.meetingDate))
				+ ", from:" + this.from
				+ ", subject:" + this.subject + ']';
	}
}
