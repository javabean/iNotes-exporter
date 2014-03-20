/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.IOException;

import fr.cedrik.inotes.fs.BaseFsExport;

/**
 * @see "http://en.wikipedia.org/wiki/Mbox"
 * @see "http://tools.ietf.org/html/rfc4155"
 * @see "http://www.qmail.org/man/man5/mbox.html"
 * @see "http://homepage.ntlworld.com./jonathan.deboynepollard/FGA/mail-mbox-formats.html"
 *
 * @author C&eacute;drik LIME
 */
abstract class BaseMBox extends BaseFsExport implements fr.cedrik.inotes.MainRunner.Main {
	public static final String MIME_TYPE = "application/mbox";//$NON-NLS-1$ // http://tools.ietf.org/html/rfc4155
	public static final String EXTENSION_MBOX = ".mbox";//$NON-NLS-1$

	public BaseMBox() throws IOException {
	}

	@Override
	protected void help() {
		System.out.println("Usage: "+this.getClass().getSimpleName()+" <out_file> [oldest message to fetch date: " + ISO8601_DATE_SEMITIME + " [newest message to fetch date: " + ISO8601_DATE_SEMITIME + " [--delete]]]");
	}

	@Override
	protected boolean prepareDestinationObjects(String baseName, String extension) {
		return true;
	}

	@Override
	protected boolean shouldLoadOldestMessageToFetchFromPreferences() {
		return writer.exists();
	}

}
