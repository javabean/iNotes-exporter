/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import fr.cedrik.inotes.BaseINotesMessage;

/**
 * @author C&eacute;drik LIME
 * @deprecated This format can loose information. Use {@link MBoxrd} instead.
 */
@Deprecated
public class MBoxo extends BaseMBox {
	public static final String EXTENSION_MBOXO = ".mboxo";//$NON-NLS-1$

	public MBoxo() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MBoxo().run(args, EXTENSION_MBOXO);
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
		writeFromLine(mbox, message);
		while (mime.hasNext()) {
			String line = mime.next();
			if (line.startsWith("From ")) {
				logger.trace("Escaping {}", line);
				mbox.write('>');
			}
			mbox.append(line).append(newLine());
		}
		mbox.write(newLine());
	}
}
