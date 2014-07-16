/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.pop3.State;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class QUIT extends BasePOP3Command implements POP3Command {

	public QUIT() {
	}

	@Override
	public boolean isValid(Context context) {
		return true;
	}

	@Override
	public State nextState(Context context) {
		return State.UPDATE;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		switch (context.state) {
		case AUTHORIZATION:
			context.inputArgs = null;
			return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("good bye!"));

		case TRANSACTION:
			String message;
			boolean logout = context.remoteSession.logout();
			if (logout) {
				message = ResponseStatus.POSITIVE.toString(context.userName + " signing off");
				context.state = nextState(context);
				context.inputArgs = null;
			} else {
				message = ResponseStatus.NEGATIVE.toString(" error while signing off " + context.userName);
			}
			return new IteratorChain<String>(message);

		case UPDATE:
			context.inputArgs = null;
			return new IteratorChain<String>(ResponseStatus.POSITIVE.toString());

		default:
			throw new IllegalStateException(context.state.toString());
		}
	}

}
