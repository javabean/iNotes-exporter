/**
 *
 */
package fr.cedrik.inotes.fs.maildir;

import java.io.File;
import java.io.IOException;

/**
 * EML == <a href="http://tools.ietf.org/html/rfc5322">RFC 5322</a> format == maildir / MH single email
 * (MSG is a binary format)
 *
 * @author C&eacute;drik LIME
 */
public class EML extends MH implements fr.cedrik.inotes.MainRunner.Main {
	public static final String MIME_TYPE = "message/rfc822";//$NON-NLS-1$

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
	protected boolean prepareDestinationObjects(String baseName, String extension) {
		String dirName = baseName;
		try {
			this.writer = new EMLWriter(new File(dirName));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
