/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class RSET extends BasePOP3Command implements POP3Command {

	public RSET() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		context.iNotesSession.undeleteAllMessages();
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString());
	}

}
