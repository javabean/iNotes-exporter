/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.MailParseException;

import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.spi.Message;
import fr.cedrik.util.IteratorChain;

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
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("bad number of lines"));
		}
		if (requestedLinesNumber < 0) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("must supply a non-negative number of lines"));
		}
		MessagesMetaData<?> messages = context.remoteSession.getMessagesMetaData();
		if (requestedMessageNumber > messages.entries.size()) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such message, only " + messages.entries.size() + " messages in maildrop"));
		}
		// TODO may NOT refer to a message marked as deleted
		Message message = messages.entries.get(requestedMessageNumber - 1);
		List<String> emptyLine = new ArrayList<String>(1);
		emptyLine.add("");
		if (requestedLinesNumber == 0) {
			Iterator<String> mimeHeaders;
			try {
				mimeHeaders = context.remoteSession.getMessageMIMEHeaders(message);
			} catch (MailParseException mpe) {
				mimeHeaders = null;
			}
			if (mimeHeaders == null) {
				return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("unknown error: can not retrieve message headers"));
			}
			String responseStatus = ResponseStatus.POSITIVE.toString("top " + requestedLinesNumber + " lines of message " + message.getId() + " follows");
			return new IteratorChain<String>(responseStatus, mimeHeaders, emptyLine.iterator());
		} else {
			// Can not simply count the number of lines in the header to finally fetch the full message body,
			// as mime headers are slightly different between those (yeah, Notes rocks!)...
			// We must therefore manually search for header/body separation and count the lines... :-(
			IteratorChain<String> mimeMessage;
			try {
				mimeMessage = context.remoteSession.getMessageMIME(message);
			} catch (MailParseException mpe) {
				mimeMessage = null;
			}
			if (mimeMessage == null) {
				return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("unknown error: can not retrieve message"));
			}
			List<String> mimeHeaders = new ArrayList<String>(64);
			int headerLinesCount = 0;
			while (mimeMessage.hasNext()) {
				String mimeHeaderLine = mimeMessage.next();
				mimeHeaders.add(mimeHeaderLine);
				++headerLinesCount;
				if (StringUtils.isEmpty(mimeHeaderLine)) {
					// end of headers
					break;
				}
			}
			mimeMessage.setMaxElementsCap(headerLinesCount + requestedLinesNumber);
			String responseStatus = ResponseStatus.POSITIVE.toString("top " + requestedLinesNumber + " lines of message " + message.getId() + " follows");
			return new IteratorChain<String>(responseStatus, mimeHeaders.iterator(), mimeMessage, emptyLine.iterator());
		}
	}

}
