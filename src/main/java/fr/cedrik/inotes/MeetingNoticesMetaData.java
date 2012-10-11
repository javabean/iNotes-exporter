/**
 *
 */
package fr.cedrik.inotes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author C&eacute;drik LIME
 */
public class MeetingNoticesMetaData {
	// viewentries
	public List<MeetingNoticeMetaData> entries = new ArrayList<MeetingNoticeMetaData>();
	// dbquotasize//dbQuotaInfo
	public int dbsize;//DBN
	public int sizelimit;//DVL
	public int warning;//CuL
	public int ignorequota;//ignoreQuota
	public int currentusage;//currentUsage

	/**
	 *
	 */
	public MeetingNoticesMetaData() {
	}

}
