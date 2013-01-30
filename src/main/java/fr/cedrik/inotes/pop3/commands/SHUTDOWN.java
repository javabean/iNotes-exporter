/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.POP3Server;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class SHUTDOWN extends BasePOP3Command implements POP3Command {

	public SHUTDOWN() {
	}

	@Override
	public boolean isValid(Context context) {
		return true;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		String magicPassword = context.inputArgs;
		if (context.pop3Properties.getPOP3ShutdownSecret().equals(magicPassword)) {
			logger.warn("Shutting down POP3 server on command from " + context.userName);
			POP3Server.shutdown();
		} else {
			logger.warn("Invalid POP3 server shutdown command secret from " + context.userName);
		}
		// no else: we don't want to give any indication of "password" failure...
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("good bye!"));
	}

}
