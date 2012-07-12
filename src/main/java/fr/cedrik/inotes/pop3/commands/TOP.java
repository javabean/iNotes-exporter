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
			return new StatusLineIterator(ResponseStatus.NEGATIVE.toString("TOP msg n"), null);
		}
		try {
			requestedMessageNumber = Integer.parseInt(tokenizer.nextToken());
		} catch (NumberFormatException noInput) {
			return new StatusLineIterator(ResponseStatus.NEGATIVE.toString("no such message"), null);
		}
		try {
			requestedLinesNumber = Integer.parseInt(tokenizer.nextToken());
		} catch (NumberFormatException noInput) {
			return new StatusLineIterator(ResponseStatus.NEGATIVE.toString("number of lines must be >= 0"), null);
		}
		if (requestedLinesNumber != 0) {
			//FIXME
			return new StatusLineIterator(ResponseStatus.NEGATIVE.toString("unimplemented; number of lines must be == 0 for now"), null);
		}
		MessagesMetaData messages = context.iNotesSession.getMessagesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new LineIterator(new StringReader(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop")));
		}
		// TODO may NOT refer to a message marked as deleted
		MessageMetaData message = messages.entries.get(requestedMessageNumber - 1);
		List<String> response = new ArrayList<String>(128);
		response.add(ResponseStatus.POSITIVE.toString("top " + requestedLinesNumber + " lines of message " + message.unid + " follows"));
		Iterator<String> mimeHeaders = context.iNotesSession.getMessageMIMEHeaders(message);
		while (mimeHeaders.hasNext()) {
			String mimeHeader = mimeHeaders.next();
			response.add(mimeHeader);
		}
		response.add("");
		// TODO requestedLinesNumber lines of body
		return response.iterator();
	}

}
