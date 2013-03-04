/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author C&eacute;drik LIME
 */
public interface POP3Command {

	boolean isValid(Context context);

	State nextState(Context context);

	Iterator<String> call(Context context) throws IOException;
}
