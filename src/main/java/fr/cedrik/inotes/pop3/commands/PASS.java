/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.pop3.State;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class PASS extends BasePOP3Command implements POP3Command {

	public PASS() {
	}

	@Override
	public boolean isValid(Context context) {
		return context.state == State.AUTHORIZATION && StringUtils.isNotBlank(context.userName);
	}

	@Override
	public State nextState(Context context) {
		return (StringUtils.isBlank(context.userName) || StringUtils.isBlank(context.userPassword)) ? State.AUTHORIZATION : State.TRANSACTION;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		if (StringUtils.isBlank(context.inputArgs)) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString());
		}
		if (StringUtils.isBlank(context.userName)) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("must call USER first"));
		}
		context.userPassword = context.inputArgs;
		if (context.iNotesSession.login(context.userName, context.userPassword)) {
			context.state = nextState(context);
			return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("welcome, " + context.userName));
		} else {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("invalid user or password"));
		}
	}

}
