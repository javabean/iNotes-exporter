/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.MessageMetaData;
import fr.cedrik.inotes.MessagesMetaData;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;

/**
 * @author C&eacute;drik LIME
 */
public class STAT extends BasePOP3Command implements POP3Command {

	public STAT() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		MessagesMetaData messages = context.iNotesSession.getMessagesMetaData();
		long totalSize = 0;
		for (MessageMetaData message : messages.entries) {
			totalSize += message.size;
		}
		return new StatusLineIterator(ResponseStatus.POSITIVE.toString("" + messages.entries.size() + ' ' + totalSize), null);
	}

}
