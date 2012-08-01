/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cedrik.inotes.Folder;
import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;

/**
 * List available folders
 * @author C&eacute;drik LIME
 */
public class FOLDERS extends BasePOP3Command implements POP3Command {

	public FOLDERS() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		List<Folder> folders = context.iNotesSession.getFolders();
		List<String> response = new ArrayList<String>(folders.size()+1);
		response.add(ResponseStatus.POSITIVE.toString("folders listing follows: " + folders.size() + " folders"));
		for (Folder folder : folders) {
			response.add(folder.toString());
		}
		return response.iterator();
	}

}
