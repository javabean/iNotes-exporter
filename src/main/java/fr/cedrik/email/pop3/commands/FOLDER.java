/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import fr.cedrik.email.FoldersList;
import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.spi.Folder;
import fr.cedrik.util.IteratorChain;

/**
 * List available folders and change current folder
 * @author C&eacute;drik LIME
 */
public class FOLDER extends BasePOP3Command implements POP3Command {

	public FOLDER() {
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		FoldersList folders = context.remoteSession.getFolders();
		if (StringUtils.isBlank(context.inputArgs)) {
			// no arguments: list available folders
			List<String> response = new ArrayList<String>(folders.size()+1);
			response.add(ResponseStatus.POSITIVE.toString("folders listing follows: " + folders.size() + " folders"));
			for (Folder folder : folders) {
				response.add(folder.toString());
			}
			return response.iterator();
		} else {
			// argument: change folder
			Folder requestedFolder = null;
			for (Folder folder : folders) {
				if (folder.getId().equals(context.inputArgs)) {
					requestedFolder = folder;
					break;
				}
			}
			if (requestedFolder == null) {
				return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such folder"));
			}
			context.remoteSession.setCurrentFolder(requestedFolder);
			return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("current folder changed to: " + requestedFolder.getName()));
		}
	}

}
