/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;

import fr.cedrik.inotes.MessageMetaData;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrd extends BaseMBox {

	public MBoxrd(File out, Date oldestMessageToFetch) throws IOException {
		super(out, oldestMessageToFetch);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			help();
			System.exit(-1);
		}
		String fileName = args[0];
		if (! fileName.endsWith(".mboxrd")) {
			fileName += ".mboxrd";
		}
		File out = new File(fileName);
		Date oldestMessageToFetch = null;
		if (args.length > 1) {
			try {
				oldestMessageToFetch = new SimpleDateFormat(ISO8601_DATE_SEMITIME).parse(args[1]);
			} catch (ParseException ignore) {
				System.out.println("Bad date format. Please use " + ISO8601_DATE_SEMITIME);
			}
		}
		new MBoxrd(out, oldestMessageToFetch).run();
	}

	protected static void help() {
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
