/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.pop3.State;

/**
 * @author C&eacute;drik LIME
 */
public class CAPA extends BasePOP3Command implements POP3Command {

	public CAPA() {
	}

	@Override
	public boolean isValid(Context context) {
		return context.state == State.AUTHORIZATION || context.state == State.TRANSACTION;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		List<String> result = new ArrayList<String>(16);
		result.add(ResponseStatus.POSITIVE.toString("capability list follows"));
		ServiceLoader<POP3Command> commands = ServiceLoader.load(POP3Command.class);
		for (POP3Command command : commands) {
			// FIXME Each capability name MAY be followed by a single space and a space-separated list of parameters.
			result.add(command.getClass().getSimpleName());
		}
		// hack for additional information
		result.add("LOGIN-DELAY 30");
		result.add("IMPLEMENTATION iNotes");
		result.add("RESP-CODES");
		result.add("AUTH-RESP-CODE");
		return result.iterator();
	}

}
