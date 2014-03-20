/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.File;
import java.io.IOException;

import fr.cedrik.inotes.BaseINotesMessage;

/**
 * @author C&eacute;drik LIME
 */
public class EMLWriter extends MHWriter {

	public static final String EXTENSION_EML = ".eml";//$NON-NLS-1$

	public EMLWriter(File mailDir) throws IOException {
		super(mailDir);
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
