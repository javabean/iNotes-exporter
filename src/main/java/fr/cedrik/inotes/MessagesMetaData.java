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
	// dbquotasize
	public int dbsize;
	public int sizelimit;
	public int warning;
	public int ignorequota;
	public int currentusage;
	// unreadinfo
	public String foldername;
	public int unreadcount;

	/**
	 *
	 */
	public MessagesMetaData() {
	}

}
