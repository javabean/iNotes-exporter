/**
 *
 */
package fr.cedrik.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import fr.cedrik.email.spi.SessionSupplier;

/**
 * @author C&eacute;drik LIME
 */
public final class ServiceLoaderUtil {

	private ServiceLoaderUtil() {
		assert false;
	}

	public static <S> S getSingle(Class<S> service) throws IllegalStateException {
		S result;
		Iterator<S> iterator = ServiceLoader.load(service).iterator();
		if (! iterator.hasNext()) {
			throw new IllegalStateException("Can not find any ServiceProvider for class: " + SessionSupplier.class.getName());
		}
		result = iterator.next();
		if (iterator.hasNext()) {
			throw new IllegalStateException("Multiple providers for ServiceProvider for class: " + SessionSupplier.class.getName());
		}
		return result;
	}

	public static <S> List<S> getAll(Class<S> service) {
		List<S> result = new ArrayList<S>();
		for (S s : ServiceLoader.load(service)) {
			result.add(s);
		}
		return result;
	}
}
