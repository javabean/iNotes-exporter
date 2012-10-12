/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class QUOTA extends BasePOP3Command implements POP3Command {

	public QUOTA() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		INotesMessagesMetaData<?> messages = context.iNotesSession.getMessagesMetaData(0);
		/*
		<dbquotasize>
			<dbsize>121938</dbsize>
			<sizelimit>1280000</sizelimit>
			<warning>768000</warning>
			<ignorequota>0</ignorequota>
			<currentusage>121938</currentusage>
		</dbquotasize>
		 */
		return new IteratorChain<String>(
				ResponseStatus.POSITIVE.toString("dbsize: " + messages.dbsize
						+ " currentusage: " + messages.currentusage
						+ " warning: " + messages.warning
						+ " sizelimit: " + messages.sizelimit
						+ " ignorequota: " + messages.ignorequota
				));
	}

}
