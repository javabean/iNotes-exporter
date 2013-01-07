/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.State;

/**
 * @author C&eacute;drik LIME
 */
abstract class BasePOP3Command implements POP3Command {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());

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
