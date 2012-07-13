/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;

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

	@Override
	protected void help() {
		System.out.println("Usage: "+MBoxrd.class.getSimpleName()+" <out_file> [oldest message to fetch date: " + ISO8601_DATE_SEMITIME + ']');
	}

	@Override
	protected void writeMIME(MessageMetaData message, LineIterator mime) throws IOException {
		writeFromLine(message);
		writeINotesData(message);
		while (mime.hasNext()) {
			String line = mime.nextLine();
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
