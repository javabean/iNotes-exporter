/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class NOOP extends BasePOP3Command implements POP3Command {

	public NOOP() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString());
	}

}
