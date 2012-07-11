/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.LineIterator;

import fr.cedrik.inotes.MessageMetaData;

/**
 * @author C&eacute;drik LIME
 * @deprecated This format can loose information. Use {@link MBoxrd} instead.
 */
@Deprecated
public class MBoxo extends BaseMBox {

	public MBoxo(File out, Date oldestMessageToFetch) throws IOException {
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
		if (! fileName.endsWith(".mboxo")) {
			fileName += ".mboxo";
		}
		File out = new File(fileName);
		Date oldestMessageToFetch = null;
		if (args.length > 1) {
			try {
				oldestMessageToFetch = new SimpleDateFormat(ISO8601_DATE).parse(args[1]);
			} catch (ParseException ignore) {
				System.out.println("Bad date format. Please use " + ISO8601_DATE);
			}
		}
		new MBoxo(out, oldestMessageToFetch).run();
	}

	protected static void help() {
		System.out.println("Usage: "+MBoxo.class.getSimpleName()+" <out_file> [oldest message to fetch date: " + ISO8601_DATE + ']');
	}

	@Override
	protected void writeMIME(MessageMetaData message, LineIterator mime) throws IOException {
		writeFromLine(message);
		while (mime.hasNext()) {
			String line = mime.nextLine();
			if (line.startsWith("From ")) {
				logger.trace("Escaping {}", line);
				mbox.write('>');
			}
			mbox.append(line).append('\n');
		}
		mbox.write('\n');
	}
}
