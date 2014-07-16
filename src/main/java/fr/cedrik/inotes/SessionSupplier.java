/**
 *
 */
package fr.cedrik.inotes;

import fr.cedrik.email.spi.Session;
import fr.cedrik.util.ExtendedProperties;

/**
 * @author C&eacute;drik LIME
 */
public class SessionSupplier implements fr.cedrik.email.spi.SessionSupplier {

	public SessionSupplier() {
	}

	@Override
	public Session apply(ExtendedProperties properties) {
		INotesProperties iNotesProperties;
		if (properties instanceof INotesProperties) {
			iNotesProperties = (INotesProperties) properties;
		} else {
			iNotesProperties = new INotesProperties(properties);
		}
		return new fr.cedrik.inotes.Session(iNotesProperties);
	}
}
