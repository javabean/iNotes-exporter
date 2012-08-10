/**
 *
 */
package fr.cedrik.inotes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author C&eacute;drik LIME
 */
public class MessagesMetaData {
	// viewentries
	public List<MessageMetaData> entries = new ArrayList<MessageMetaData>();
	// dbquotasize//dbQuotaInfo
	public int dbsize;//DBN
	public int sizelimit;//DVL
	public int warning;//CuL
	public int ignorequota;//ignoreQuota
	public int currentusage;//currentUsage
	// unreadinfo
	public String foldername;
	public int unreadcount;

	/**
	 *
	 */
	public MessagesMetaData() {
	}

}
