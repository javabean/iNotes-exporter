/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.LineIterator;

import fr.cedrik.inotes.MessageMetaData;
import fr.cedrik.inotes.MessagesMetaData;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class TOP extends BasePOP3Command implements POP3Command {

	public TOP() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		int requestedMessageNumber = -1;
		int requestedLinesNumber = -1;
		StringTokenizer tokenizer = new StringTokenizer(context.inputArgs);
		if (tokenizer.countTokens() != 2) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("TOP msg n"));
		}
		try {
			requestedMessageNumber = Integer.parseInt(tokenizer.nextToken());
		} catch (NumberFormatException noInput) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message"));
		}
		try {
			requestedLinesNumber = Integer.parseInt(tokenizer.nextToken());
		} catch (NumberFormatException noInput) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("number of lines must be >= 0"));
		}
		if (requestedLinesNumber != 0) {
			//FIXME
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("unimplemented; number of lines must be == 0 for now"));
		}
		MessagesMetaData messages = context.iNotesSession.getMessagesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"));
		}
		// TODO may NOT refer to a message marked as deleted
		MessageMetaData message = messages.entries.get(requestedMessageNumber - 1);
		Iterator<String> mimeHeaders = context.iNotesSession.getMessageMIMEHeaders(message);
		if (mimeHeaders == null) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("unknown error: can not retrieve message headers"));
		} else {
			String responseStatus = ResponseStatus.POSITIVE.toString("top " + requestedLinesNumber + " lines of message " + message.unid + " follows");
			List<String> emptytLine = new ArrayList<String>(1);
			emptytLine.add("");
			// TODO requestedLinesNumber lines of body
			return new IteratorChain<String>(responseStatus, mimeHeaders, emptytLine.iterator());
		}
	}

}
