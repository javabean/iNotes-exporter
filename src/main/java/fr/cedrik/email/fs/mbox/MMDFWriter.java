/**
 *
 */
package fr.cedrik.email.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import fr.cedrik.email.spi.Message;

/**
 * @author C&eacute;drik LIME
 */
public class MMDFWriter extends BaseMBoxWriter {

	private static final String SEPARATOR_MARK = "\0x1\0x1\0x1\0x1";//$NON-NLS-1$ // four characters "^A^A^A^A" (Control-A; ASCII 1)

	public MMDFWriter(String baseName, String extension) {
		super(baseName, extension);
	}

	@Override
	protected void writeMIME(Writer mbox, Message message, Iterator<String> mime) throws IOException {
		mbox.append(SEPARATOR_MARK).append(newLine());
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append(newLine());
		}
		mbox.append(SEPARATOR_MARK).append(newLine());
	}

}
