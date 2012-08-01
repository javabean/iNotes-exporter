/**
 *
 */
package fr.cedrik.inotes.pop3;

import fr.cedrik.inotes.INotesProperties;

/**
 * @author C&eacute;drik LIME
 */
public class Context {
	public State state = State.AUTHORIZATION;
	public String inputArgs;
	public fr.cedrik.inotes.Session iNotesSession = new fr.cedrik.inotes.Session(new INotesProperties());
	public String userName;
	public String userPassword;

	public Context() {
	}

}
