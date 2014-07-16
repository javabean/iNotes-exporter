/**
 *
 */
package fr.cedrik.inotes;

import java.util.Date;

import fr.cedrik.email.spi.Message;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseINotesMessage implements Message {
	public String unid;
	public String noteid;

	/**
	 *
	 */
	public BaseINotesMessage() {
	}

	@Override
	public String getId() {
		return unid;
	}

	@Override
	public abstract Date getDate();

	@Override
	public int getSize() {
		return -1;
	}
}
