/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;

import fr.cedrik.inotes.Folder;
import fr.cedrik.inotes.FoldersList;
import fr.cedrik.inotes.INotesProperties;
import fr.cedrik.inotes.Session;

/**
 * @author C&eacute;drik LIME
 */
public class MailDirPP extends BaseMailDir {
	protected File baseMailDir;

	public MailDirPP() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MailDirPP().run(args, null);
	}

	/**
	 * @param args
	 */
	@Override
	public void _main(String[] args) throws IOException {
		main(args);
	}

	@Override
	protected void help() {
		System.out.println("Usage: "+MailDirPP.class.getSimpleName()+" <out_dir>");
	}

	@Override
	protected void run(String[] args, String extension) throws IOException {
		if (args.length != 1) {
			help();
			System.exit(-1);
		}
		if (! validateDestinationName(args[0], extension)) {
			return;
		}
		assert mailDir != null;
		baseMailDir = mailDir;
		iNotes = new INotesProperties(INotesProperties.FILE);
		iNotes.setNotesFolderId(Folder.INBOX);
		session = new Session(iNotes);
		// login
		if (! session.login(iNotes.getUserName(), iNotes.getUserPassword())) {
			logger.error("Can not login user {}!", iNotes.getUserName());
			return;
		}
		try {
			// export folders hierarchy
			FoldersList folders = session.getFolders();
			//FIXME check no 2 folders have the same .name hierarchy
			for (Folder folder : folders) {
				if (folder.name.startsWith(".")) {
					throw new IllegalArgumentException("Folder can not start with a '.': " + folder);
				}
				if (folder.isAllMails()) {
					continue;
				}
				if (! validateDestinationName(computeMaildirFolderName(folder, folders), extension)) {
					continue;
				}
				export(folder, args);
			}
		} finally {
			session.logout();
		}
	}

	protected String computeMaildirFolderName(Folder folder, FoldersList folders) {
		if (folder.isInbox()) {
			return baseMailDir.getPath();
		} else {
			// compute MailDir++ full folder name and encode each segment
			List<Folder> foldersChain = folders.getFoldersChain(folder);
			StringBuilder result = new StringBuilder();
			result.append('.');
			for (Folder parent : foldersChain) {
				result.append(encodeFolderName(parent.name));
				result.append('.');
			}
			result.deleteCharAt(result.length() - 1); // remove trailing '.'
			return new File(baseMailDir, result.toString()).getPath();
		}
	}

	/**
	 * @see RFC2060 5.1.3.  Mailbox International Naming Convention  + special treatment for '.' and '/' as per MailDir++ specification
	 */
	protected String encodeFolderName(String folderName) {
		return BASE64MailboxEncoder.encode(folderName).replace(".", "&AC4-").replace("/", "&AC8-");
	}
}
