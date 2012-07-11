/**
 *
 */
package fr.cedrik.inotes.pop3;

/**
 * @author C&eacute;drik LIME
 */
public class Context {
	public State state = State.AUTHORIZATION;
	public String inputArgs;
	public fr.cedrik.inotes.Session iNotesSession = new fr.cedrik.inotes.Session();
	public String userName;
	public String userPassword;

	public Context() {
	}

}
