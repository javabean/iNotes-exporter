/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;

/**
 * @author C&eacute;drik LIME
 */
public class NOOP extends BasePOP3Command implements POP3Command {

	public NOOP() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		return new StatusLineIterator(ResponseStatus.POSITIVE.toString(), null);
	}

}
