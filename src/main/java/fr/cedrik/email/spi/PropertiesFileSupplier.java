/**
 *
 */
package fr.cedrik.email.spi;

import fr.cedrik.email.EMailProperties;
import fr.cedrik.util.ServiceLoaderUtil;


/**
 * @author C&eacute;drik LIME
 */
public interface PropertiesFileSupplier<P extends EMailProperties> {
	P get();

	public static class Util {
		private static final PropertiesFileSupplier SINGLETON;

		static {
			SINGLETON = ServiceLoaderUtil.getSingle(PropertiesFileSupplier.class);
		}

		public static EMailProperties get() {
			return SINGLETON.get();
		}
	}
}
