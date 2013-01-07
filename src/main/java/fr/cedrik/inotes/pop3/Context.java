/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.net.InetAddress;



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
	private final InetAddress clientSocketAddress;

	public Context(InetAddress clientSocketAddress, POP3Properties pop3Properties) {
		this.clientSocketAddress = clientSocketAddress;
		this.pop3Properties = pop3Properties;
		this.iNotesSession = new fr.cedrik.inotes.Session(pop3Properties);
	}

	/**
	 * #see {@link LockOutFilter#isLocked(Object)}
	 */
	public boolean isLocked() {
		return POP3Server.lockOutFilter.isLocked(clientSocketAddress) || POP3Server.lockOutFilter.isLocked(getUserNameAndServer());
	}

	/**
	 * #see {@link LockOutFilter#registerAuthSuccess(Object)}
	 */
	public void registerAuthSuccess() {
		POP3Server.lockOutFilter.registerAuthSuccess(getUserNameAndServer());
		POP3Server.lockOutFilter.registerAuthSuccess(clientSocketAddress);
	}

	/**
	 * #see {@link LockOutFilter#registerAuthFailure(Object)}
	 */
	public void registerAuthFailure() {
		POP3Server.lockOutFilter.registerAuthFailure(getUserNameAndServer());
		POP3Server.lockOutFilter.registerAuthFailure(clientSocketAddress);
	}

	private String getUserNameAndServer() {
		return userName + '@' + pop3Properties.getServerAddress();
	}
}
