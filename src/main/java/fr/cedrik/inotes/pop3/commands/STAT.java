/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class STAT extends BasePOP3Command implements POP3Command {

	public STAT() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		INotesMessagesMetaData<?> messages = context.iNotesSession.getMessagesAndMeetingNoticesMetaData();
		long totalSize = 0;
		for (BaseINotesMessage message : messages.entries) {
			totalSize += message.getSize();
		}
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("" + messages.entries.size() + ' ' + totalSize));
	}

}
