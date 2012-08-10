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
	public POP3Properties pop3Properties;
	public fr.cedrik.inotes.Session iNotesSession;
	public String userName;
	public String userPassword;

	public Context(POP3Properties pop3Properties) {
		this.pop3Properties = pop3Properties;
		this.iNotesSession = new fr.cedrik.inotes.Session(pop3Properties);
	}

}
