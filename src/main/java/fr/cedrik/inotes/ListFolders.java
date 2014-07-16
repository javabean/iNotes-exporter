/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author C&eacute;drik LIME
 */
public class ListFolders implements fr.cedrik.email.MainRunner.Main {
	private static final Logger logger = LoggerFactory.getLogger(ListFolders.class);

	public ListFolders() {
	}

	/**
	 * @param args
	 */
	@Override
	public void _main(String[] args) throws IOException {
		main(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		INotesProperties iNotes = new INotesProperties(INotesProperties.FILE);
		Session session = new Session(iNotes);
		// login
		if (! session.login(iNotes.getUserName(), iNotes.getUserPassword())) {
			logger.error("Can not login user {}!", iNotes.getUserName());
			return;
		}
		try {
			for (Folder folder : session.getFolders()) {
				System.out.println(folder);
			}
		} finally {
			session.logout();
		}
	}

}
