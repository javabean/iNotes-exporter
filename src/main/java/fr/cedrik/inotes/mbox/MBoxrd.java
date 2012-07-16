/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cedrik.inotes.MessageMetaData;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrd extends BaseMBox {

	public MBoxrd() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MBoxrd().run(args, ".mboxrd");
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
		System.out.println("Usage: "+MBoxrd.class.getSimpleName()+" <out_file> [oldest message to fetch date: " + ISO8601_DATE_SEMITIME + ']');
	}

	@Override
	protected void writeMIME(MessageMetaData message, Iterator<String> mime) throws IOException {
		writeFromLine(message);
		while (mime.hasNext()) {
			String line = mime.next();
			Matcher from_ = FROM_.matcher(line);
			if (from_.find()) {
				logger.trace("Escaping {}", from_.group());
				mbox.write('>');
			}
			mbox.append(line).append('\n');
		}
		mbox.write('\n');
	}

	private static final Pattern FROM_ = Pattern.compile("^>*From ");//$NON-NLS-1$
}
