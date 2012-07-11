/**
 *
 */
package fr.cedrik.inotes.pop3;

/**
 * @author C&eacute;drik LIME
 * @see "http://tools.ietf.org/html/rfc1939"
 */
public enum ResponseStatus {
	POSITIVE("+OK"), NEGATIVE("-ERR");

	private String status;

	private ResponseStatus(String msg) {
		this.status = msg;
	}

	@Override
	public String toString() {
		return status;
	}

	public String toString(String msg) {
		return status + ' ' + msg;
	}
}
