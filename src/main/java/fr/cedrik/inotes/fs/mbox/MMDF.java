/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import fr.cedrik.inotes.BaseINotesMessage;

/**
 * MMDF - Multi-channel Memorandum Distribution Facility mailbox format
 *
 * @author C&eacute;drik LIME
 */
public class MMDF extends BaseMBox {
	private static final String SEPARATOR_MARK = "\0x1\0x1\0x1\0x1";//$NON-NLS-1$ // four characters "^A^A^A^A" (Control-A; ASCII 1)

	public MMDF() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MMDF().run(args, ".mmdf");
	}

	/**
	 * @param args
	 */
	@Override
	public void _main(String[] args) throws IOException {
		main(args);
	}

	@Override
	protected void writeMIME(Writer mbox, BaseINotesMessage message, Iterator<String> mime) throws IOException {
		mbox.append(SEPARATOR_MARK).append('\n');
		while (mime.hasNext()) {
			String line = mime.next();
			mbox.append(line).append('\n');
		}
		mbox.append(SEPARATOR_MARK).append('\n');
	}
}
