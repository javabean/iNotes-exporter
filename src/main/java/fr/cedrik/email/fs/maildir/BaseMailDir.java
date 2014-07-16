/**
 *
 */
package fr.cedrik.email.fs.maildir;

import java.io.File;
import java.io.IOException;

import fr.cedrik.email.fs.BaseFsExport;

/**
 * @see "http://en.wikipedia.org/wiki/Maildir"
 * @see "http://www.qmail.org/man/man5/maildir.html"
 * @see "http://cr.yp.to/proto/maildir.html"
 *
 * @author C&eacute;drik LIME
 */
abstract class BaseMailDir extends BaseFsExport implements fr.cedrik.email.MainRunner.Main {

	public BaseMailDir() throws IOException {
	}

	@Override
	protected boolean prepareDestinationObjects(String baseName, String extension) {
		String dirName = baseName;
		try {
			this.writer = new BaseMailDirWriter(new File(dirName));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	protected boolean shouldLoadOldestMessageToFetchFromPreferences() {
		return writer.exists();
	}

}
