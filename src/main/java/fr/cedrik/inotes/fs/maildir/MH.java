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

/**
 * @author C&eacute;drik LIME
 */
public class MH extends MailDirPP implements fr.cedrik.inotes.MainRunner.Main {

	public MH() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MH().run(args, null);
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
		System.out.println("Usage: "+MH.class.getSimpleName()+" <out_dir>");
	}

	@Override
	protected boolean prepareDestinationObjects(String baseName, String extension) {
		String dirName = baseName;
		try {
			this.writer = new MHWriter(new File(dirName));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	protected String computeMaildirFolderName(Folder folder, FoldersList folders) {
		if (folder.isInbox()) {
			return baseMailDir.getPath();
		} else {
			// compute MailDir++ full folder name and encode each segment
			List<Folder> foldersChain = folders.getFoldersChain(folder);
			StringBuilder result = new StringBuilder();
			for (Folder parent : foldersChain) {
				result.append(encodeFolderName(parent.name));
				result.append(File.separatorChar);
			}
			result.deleteCharAt(result.length() - 1); // remove trailing '/'
			return new File(baseMailDir, result.toString()).getPath();
		}
	}

	/**
	 * @see RFC2060 5.1.3.  Mailbox International Naming Convention  + special treatment for '/'
	 */
	@Override
	protected String encodeFolderName(String folderName) {
		return BASE64MailboxEncoder.encode(folderName).replace("/", "&AC8-");
	}
}
