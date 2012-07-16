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
 * @author C&eacute;drik LIME
 */
public class USER extends BasePOP3Command implements POP3Command {

	public USER() {
	}

	@Override
	public boolean isValid(Context context) {
		return context.state == State.AUTHORIZATION && StringUtils.isBlank(context.userName);
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
		StringTokenizer tokenizer = new StringTokenizer(context.inputArgs, "@", false);
		if (tokenizer.countTokens() != 2) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("bad user format; should be user@https://webmail.example.com"));
		}
		context.userName = tokenizer.nextToken();
		String serverURL = tokenizer.nextToken();
		context.iNotesSession.setServerAddress(serverURL);
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString(context.userName + '@' + serverURL));
	}

}
