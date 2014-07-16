/**
 *
 */
package fr.cedrik.inotes;


/**
 * @author C&eacute;drik LIME
 */
public class PropertiesFileSupplier implements fr.cedrik.email.spi.PropertiesFileSupplier<INotesProperties> {

	public PropertiesFileSupplier() {
	}

	@Override
	public INotesProperties get() {
		return new INotesProperties(INotesProperties.FILE);
	}
}
