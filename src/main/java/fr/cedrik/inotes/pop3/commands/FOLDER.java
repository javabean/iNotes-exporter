/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import fr.cedrik.inotes.Folder;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * Change current folder
 * @author C&eacute;drik LIME
 */
public class FOLDER extends BasePOP3Command implements POP3Command {

	public FOLDER() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		List<Folder> folders = context.iNotesSession.getFolders();
		Folder requestedFolder = null;
		for (Folder folder : folders) {
			if (folder.id.equals(context.inputArgs)) {
				requestedFolder = folder;
				break;
			}
		}
		if (requestedFolder == null) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such folder"));
		}
		context.iNotesSession.setCurrentFolder(requestedFolder);
		return new IteratorChain<String>(ResponseStatus.POSITIVE.toString());
	}

}
