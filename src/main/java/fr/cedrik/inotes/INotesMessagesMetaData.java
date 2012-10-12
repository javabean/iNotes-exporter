/**
 *
 */
package fr.cedrik.inotes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author C&eacute;drik LIME
 */
public class INotesMessagesMetaData<E extends BaseINotesMessage> implements Cloneable {
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


	public INotesMessagesMetaData() {
	}

	@Override
	public INotesMessagesMetaData<BaseINotesMessage> clone() {
		try {
			INotesMessagesMetaData<BaseINotesMessage> clone = (INotesMessagesMetaData<BaseINotesMessage>) super.clone();
			// we do not want to clone the entries
			clone.entries = new ArrayList<BaseINotesMessage>();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
