/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.State;

/**
 * @author C&eacute;drik LIME
 */
abstract class BasePOP3Command implements POP3Command {

	public BasePOP3Command() {
	}

	@Override
	public boolean isValid(Context context) {
		return context.state == State.TRANSACTION;
	}

	@Override
	public State nextState(Context context) {
		assert isValid(context);
		return context.state;
	}
}
