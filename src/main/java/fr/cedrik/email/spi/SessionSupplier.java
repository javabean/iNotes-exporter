/**
 *
 */
package fr.cedrik.email.spi;

import fr.cedrik.util.ExtendedProperties;
import fr.cedrik.util.ServiceLoaderUtil;


/**
 * @author C&eacute;drik LIME
 */
public interface SessionSupplier /* implements Function<ExtendedProperties, Session> */ {
	Session apply(ExtendedProperties properties);

	public static class Util {
		private static final SessionSupplier SINGLETON;

		static {
			SINGLETON = ServiceLoaderUtil.getSingle(SessionSupplier.class);
		}

		public static Session get(ExtendedProperties properties) {
			return SINGLETON.apply(properties);
		}
	}
}
