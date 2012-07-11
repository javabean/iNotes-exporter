/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.POP3Properties;
import fr.cedrik.inotes.pop3.POP3Server;
import fr.cedrik.inotes.pop3.ResponseStatus;

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
		if (POP3Properties.getInstance().getShutdownSecret().equals(magicPassword)) {
			POP3Server.shutdown();
		}
		// no else: we don't want to give any indication of "password" failure...
		return new StatusLineIterator(ResponseStatus.POSITIVE.toString("good bye!"), null);
	}

}
