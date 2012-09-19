/**
 *
 */
package fr.cedrik.inotes;

/**
 * @author C&eacute;drik LIME
 */
public class Folder {
	// Special folders ids
	public static final String INBOX          = "($Inbox)";//$NON-NLS-1$
	public static final String DRAFTS         = "($Drafts)";//$NON-NLS-1$
	public static final String SENT           = "($Sent)";//$NON-NLS-1$
	public static final String FOLLOW_UP      = "($Follow-Up)";//$NON-NLS-1$
	public static final String ALL            = "($All)";//$NON-NLS-1$
	public static final String JUNKMAIL       = "($JunkMail)";//$NON-NLS-1$
	public static final String SOFT_DELETIONS = "($SoftDeletions)"; // maildir++ name: .Trash //$NON-NLS-1$

	public String id;
	public String name;
	public int    levelNumber;
	public String levelTree;

	public Folder() {
	}

	public boolean isInbox() {
		return INBOX.equals(id);
	}

	public boolean isAllMails() {
		return ALL.equals(id);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (level " + levelNumber + ", #" + levelTree + "):\t" + id + '\t' + name;
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof Folder)) {
			return false;
		}
		return ((Folder)obj).id.equals(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
