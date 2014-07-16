/**
 *
 */
package fr.cedrik.email.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cedrik.email.spi.Message;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrdWriter extends BaseMBoxWriter {

	public MBoxrdWriter(String baseName, String extension) {
		super(baseName, extension);
	}

	@Override
	protected void writeMIME(Writer mbox, Message message, Iterator<String> mime) throws IOException {
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
