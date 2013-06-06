/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.File;
import java.io.IOException;

import fr.cedrik.inotes.BaseINotesMessage;

/**
 * EML == <a href="http://tools.ietf.org/html/rfc5322">RFC 5322</a> format == maildir / MH single email
 * (MSG is a binary format)
 *
 * @author C&eacute;drik LIME
 */
public class EML extends MH implements fr.cedrik.inotes.MainRunner.Main {
	public static final String MIME_TYPE = "message/rfc822";//$NON-NLS-1$

	public static final String EXTENSION_EML = ".eml";//$NON-NLS-1$

	protected File baseMailDir;

	public EML() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new EML().run(args, null);
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
		System.out.println("Usage: "+EML.class.getSimpleName()+" <out_dir>");
	}


	@Override
	protected String getMailFileName(BaseINotesMessage message) {
		long id = message.getDate().getTime();
		while (new File(mailDir, String.valueOf(id) + EXTENSION_EML).exists()) {
			++id;
		}
		return String.valueOf(id) + EXTENSION_EML;
	}

	@Override
	protected String newLine() {
		return "\r\n";//$NON-NLS-1$
	}
}
