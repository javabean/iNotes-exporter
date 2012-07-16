/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.pop3.State;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * Note: we can not implement APOP, since this requires the (POP3) server to know the user password.
 *
 * @author C&eacute;drik LIME
 */
@Deprecated
public class APOP extends BasePOP3Command implements POP3Command {

	public APOP() {
	}

	@Override
	public boolean isValid(Context context) {
		return context.state == State.AUTHORIZATION && StringUtils.isBlank(context.userName);
	}

	@Override
	public State nextState(Context context) {
		return (StringUtils.isBlank(context.userName)) ? State.AUTHORIZATION : State.TRANSACTION;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		StringTokenizer tokenizer = new StringTokenizer(context.inputArgs);
		if (tokenizer.countTokens() != 2) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("APOP name digest"));
		}
		String name = tokenizer.nextToken();
		String digest = tokenizer.nextToken();
		//TODO
		return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("permission denied"));
	}

}
