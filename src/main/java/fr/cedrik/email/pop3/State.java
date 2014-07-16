/**
 *
 */
package fr.cedrik.email.pop3;

/**
 * @author C&eacute;drik LIME
 * @see "http://tools.ietf.org/html/rfc1939"
 */
public enum State {
	/**
	 * In this state, the client must identify itself to the POP3
	 * server.  Once the client has successfully done this, the server
	 * acquires resources associated with the client's maildrop, and the
	 * session enters the TRANSACTION state.
	 */
	AUTHORIZATION,
	/**
	 * In this state, the client
	 * requests actions on the part of the POP3 server.  When the client has
	 * issued the QUIT command, the session enters the UPDATE state.
	 */
	TRANSACTION,
	/**
	 * In this state, the POP3 server releases any resources acquired during
	 * the TRANSACTION state and says goodbye.  The TCP connection is then closed.
	 */
	UPDATE;
}
