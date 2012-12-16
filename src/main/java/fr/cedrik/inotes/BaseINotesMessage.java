/**
 *
 */
package fr.cedrik.inotes;

import java.util.Date;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseINotesMessage {
	public String unid;
	public String noteid;

	/**
	 *
	 */
	public BaseINotesMessage() {
	}

	public abstract Date getDate();

	public int getSize() {
		return -1;
	}
}
