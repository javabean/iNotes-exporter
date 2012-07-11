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
public class DELE extends BasePOP3Command implements POP3Command {

	public DELE() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		int requestedMessageNumber = -1;
		try {
			requestedMessageNumber = Integer.parseInt(context.inputArgs);
		} catch (NumberFormatException noInput) {
			return new StatusLineIterator(ResponseStatus.NEGATIVE.toString("no such message"), null);
		}
		MessagesMetaData messages = context.iNotesSession.getMessagesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new StatusLineIterator(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"), null);
		}
		// TODO may NOT refer to a message marked as deleted: -ERR message 2 already deleted
		MessageMetaData message = messages.entries.get(requestedMessageNumber - 1);
		context.iNotesSession.deleteMessage(message);
		return new StatusLineIterator(ResponseStatus.POSITIVE.toString("message " + requestedMessageNumber + " deleted"), null);
	}

}
