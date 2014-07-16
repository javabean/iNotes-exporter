/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.spi.Message;
import fr.cedrik.util.IteratorChain;

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
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message"));
		}
		MessagesMetaData<?> messages = context.remoteSession.getMessagesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"));
		}
		// TODO may NOT refer to a message marked as deleted: -ERR message 2 already deleted
		Message message = messages.entries.get(requestedMessageNumber - 1);
		context.remoteSession.deleteMessage(message);
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("message " + requestedMessageNumber + " (" + message.getId() + ") deleted"));
	}

}
