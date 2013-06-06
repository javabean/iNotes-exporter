/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cedrik.inotes.BaseINotesMessage;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrd extends BaseMBox {
	public static final String EXTENSION_MBOXRD = ".mboxrd";//$NON-NLS-1$

	public MBoxrd() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MBoxrd().run(args, EXTENSION_MBOXRD);
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
			Matcher from_ = FROM_.matcher(line);
			if (from_.find()) {
				logger.trace("Escaping {}", from_.group());
				mbox.write('>');
			}
			mbox.append(line).append(newLine());
		}
		mbox.write(newLine());
	}

	private static final Pattern FROM_ = Pattern.compile("^>*From ");//$NON-NLS-1$
}
