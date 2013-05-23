/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import org.springframework.mail.MailParseException;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class RETR extends BasePOP3Command implements POP3Command {

	public RETR() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		int requestedMessageNumber = -1;
		try {
			requestedMessageNumber = Integer.parseInt(context.inputArgs);
		} catch (NumberFormatException noInput) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message"));
		}
		INotesMessagesMetaData<? extends BaseINotesMessage> messages = context.iNotesSession.getMessagesAndMeetingNoticesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"));
		}
		// TODO may NOT refer to a message marked as deleted
		BaseINotesMessage message = messages.entries.get(requestedMessageNumber-1);
		IteratorChain<String> mimeMessage;
		try {
			mimeMessage = context.iNotesSession.getMessageMIME(message);
		} catch (MailParseException mpe) {
			mimeMessage = null;
		}
		if (mimeMessage == null) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("unknown error: can not retrieve message"));
		} else {
			return new IteratorChain<String>(
					ResponseStatus.POSITIVE.toString("message " + requestedMessageNumber + " (" + message.unid + ") follows (" + message.getSize() + " octets)"),
					mimeMessage);
		}
	}

}
