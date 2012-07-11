/**
 *
 */
package fr.cedrik.inotes.mbox;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.LineIterator;

import fr.cedrik.inotes.MessageMetaData;

/**
 * @author C&eacute;drik LIME
 * @deprecated This format can loose information. Use {@link MBoxrd} instead.
 */
@Deprecated
public class MBoxo extends BaseMBox {

	public MBoxo(File out) throws IOException {
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
		if (! fileName.endsWith(".mboxo")) {
			fileName += ".mboxo";
		}
		File out = new File(fileName);
		new MBoxo(out).run();
	}

	protected static void help() {
		System.out.println("Usage: "+MBoxo.class.getSimpleName()+" <out_file>");
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
