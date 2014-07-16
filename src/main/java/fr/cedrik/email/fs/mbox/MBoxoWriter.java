/**
 *
 */
package fr.cedrik.email.fs.mbox;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import fr.cedrik.email.spi.Message;

/**
 * @author C&eacute;drik LIME
 * @deprecated This format can loose information. Use {@link MBoxrdWriter} instead.
 */
@Deprecated
public class MBoxoWriter extends BaseMBoxWriter {

	public MBoxoWriter(String baseName, String extension) {
		super(baseName, extension);
	}

	@Override
	protected void writeMIME(Writer mbox, Message message, Iterator<String> mime) throws IOException {
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
