/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;

import fr.cedrik.inotes.BaseINotesMessage;
import fr.cedrik.inotes.Folder;
import fr.cedrik.inotes.FoldersList;
import fr.cedrik.inotes.INotesMessagesMetaData;
import fr.cedrik.inotes.INotesProperties;
import fr.cedrik.inotes.Session;
import fr.cedrik.inotes.fs.BaseFsExport;
import fr.cedrik.inotes.util.Charsets;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class MH extends BaseFsExport implements fr.cedrik.inotes.MainRunner.Main {

	protected File baseMailDir;

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

	protected File mailDir;

	@Override
	protected boolean validateDestinationName(String baseName, String extension) {
		String dirName = baseName;
		this.mailDir = new File(dirName);
		if ((this.mailDir.exists() && ! this.mailDir.isDirectory()) || (! this.mailDir.exists() && ! this.mailDir.mkdirs())) {
			logger.error("Not a directory, or can not create directory: " + mailDir);
			return false;
		}
		return true;
	}

	@Override
	protected boolean shouldLoadOldestMessageToFetchFromPreferences() {
		return mailDir.exists() && mailDir.list().length != 0;
	}

	@Override
	protected final Date export(INotesMessagesMetaData<? extends BaseINotesMessage> messages, boolean deleteExportedMessages) throws IOException {
		Date lastExportedMessageDate = null;
		// write messages
		for (BaseINotesMessage message : messages.entries) {
			IteratorChain<String> mime = session.getMessageMIME(message);
			if (mime == null || ! mime.hasNext()) {
				logger.error("Empty MIME message! ({})", message);
				break;
			}
			logger.debug("Writing message {}", message);
			// open out file
			File outFile = new File(mailDir, getMailFileName(message));
			FileOutputStream outStream = new FileOutputStream(outFile);
			FileLock outFileLock = outStream.getChannel().tryLock();
			if (outFileLock == null) {
				logger.error("Can not acquire a lock on file " + outFile + ". Aborting.");
				break;
			}
			Writer mbox = new BufferedWriter(new OutputStreamWriter(outStream, Charsets.US_ASCII), 32*1024);
			try {
				writeMIME(mbox, message, mime);
			} finally {
				mime.close();
				mbox.flush();
				outFileLock.release();
				IOUtils.closeQuietly(mbox);
			}
			// the modification time of the file is the delivery date of the message.
			outFile.setLastModified(message.getDate().getTime());
			lastExportedMessageDate = message.getDate();
		}
		if (deleteExportedMessages) {
			session.deleteMessage(messages.entries);
		}
		return lastExportedMessageDate;
	}

	protected String getMailFileName(BaseINotesMessage message) {
		long id = message.getDate().getTime();
		while (new File(mailDir, String.valueOf(id)).exists()) {
			++id;
		}
		return String.valueOf(id);
	}

	@Override
	protected void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException {
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append('\n');
		}
	}

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
	protected String encodeFolderName(String folderName) {
		return BASE64MailboxEncoder.encode(folderName).replace("/", "&AC8-");
	}
}
