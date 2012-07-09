/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import fr.cedrik.inotes.MessageMetaData;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrd extends BaseMBox {

	public MBoxrd(File out) throws IOException {
		super(out);
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
		new MBoxrd(out).run();
	}

	protected static void help() {
		System.out.println("Usage: "+MBoxrd.class.getSimpleName()+" <out_file>");
	}

	@Override
	protected void writeMIME(MessageMetaData message, String mime) throws IOException {
		writeFromLine(message);
		LineIterator lines = IOUtils.lineIterator(new StringReader(mime));
		while (lines.hasNext()) {
			String line = lines.nextLine();
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
