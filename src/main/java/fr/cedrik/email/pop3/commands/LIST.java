/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.spi.Message;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class LIST extends BasePOP3Command implements POP3Command {

	public LIST() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		int requestedMessageNumber = -1;
		try {
			requestedMessageNumber = Integer.parseInt(context.inputArgs);
		} catch (NumberFormatException noInput) {
		}
		MessagesMetaData<? extends Message> messages = context.remoteSession.getMessagesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"));
		}
		// TODO if present, may NOT refer to a message marked as deleted
		List<? extends Message> maildrop;
		if (requestedMessageNumber > 0) {
			maildrop = new ArrayList<Message>(1);
			((List<Message>)maildrop).add(messages.entries.get(requestedMessageNumber-1));
		} else {
			// TODO messages marked as deleted are not listed
			maildrop = messages.entries;
		}
		List<String> response = new ArrayList<String>(maildrop.size()+1);
		response.add(ResponseStatus.POSITIVE.toString("scan listing follows: " + maildrop.size() + " message(s)"));
		int n = 1;
		for (Message message : maildrop) {
			response.add(""+n+' '+message.getSize());
			++n;
		}
		return response.iterator();
	}

}
