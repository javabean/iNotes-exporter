/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class UIDL extends BasePOP3Command implements POP3Command {

	public UIDL() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		int requestedMessageNumber = -1;
		try {
			requestedMessageNumber = Integer.parseInt(context.inputArgs);
		} catch (NumberFormatException noInput) {
		}
		INotesMessagesMetaData<? extends BaseINotesMessage> messages = context.iNotesSession.getMessagesAndMeetingNoticesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"));
		}
		// TODO if present, may NOT refer to a message marked as deleted
		List<? extends BaseINotesMessage> maildrop;
		if (requestedMessageNumber > 0) {
			maildrop = new ArrayList<BaseINotesMessage>(1);
			((List<BaseINotesMessage>)maildrop).add(messages.entries.get(requestedMessageNumber-1));
		} else {
			// TODO messages marked as deleted are not listed
			maildrop = messages.entries;
		}
		List<String> response = new ArrayList<String>(maildrop.size()+1);
		response.add(ResponseStatus.POSITIVE.toString("unique-id listing follows: " + maildrop.size() + " message(s)"));
		int n = 1;
		for (BaseINotesMessage message : maildrop) {
			response.add(""+n+' '+message.unid);
			++n;
		}
		return response.iterator();
	}

}
