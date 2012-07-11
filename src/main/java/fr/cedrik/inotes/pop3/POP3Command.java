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

	public boolean isValid(Context context);

	public State nextState(Context context);

	public Iterator<String> call(Context context) throws IOException;
}
