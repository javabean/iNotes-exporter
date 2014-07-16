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
public class STAT extends BasePOP3Command implements POP3Command {

	public STAT() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		MessagesMetaData<? extends Message> messages = context.remoteSession.getMessagesMetaData();
		long totalSize = 0;
		for (Message message : messages.entries) {
			totalSize += message.getSize();
		}
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("" + messages.entries.size() + ' ' + totalSize));
	}

}
