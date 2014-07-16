/**
 *
 */
package fr.cedrik.email;

import java.util.ArrayList;
import java.util.List;

import fr.cedrik.email.spi.Message;

/**
 * @author C&eacute;drik LIME
 */
public class MessagesMetaData<E extends Message> implements Cloneable {
	// viewentries
	public List<E> entries = new ArrayList<E>();
	// dbquotasize//dbQuotaInfo
	public int dbsize = -1;//DBN
	public int sizelimit = -1;//DVL
	public int warning = -1;//CuL
	public int ignorequota = -1;//ignoreQuota
	public int currentusage = -1;//currentUsage
	// unreadinfo
	public String foldername;
	public int unreadcount = -1;


	public MessagesMetaData() {
	}

	@Override
	public MessagesMetaData<Message> clone() {
		try {
			MessagesMetaData<Message> clone = (MessagesMetaData<Message>) super.clone();
			// we do not want to clone the entries
			clone.entries = new ArrayList<Message>();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
