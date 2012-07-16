/**
 *
 */
package fr.cedrik.inotes;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author C&eacute;drik LIME
 */
public class MainRunner {

	public static interface Main {
		public void _main(String[] args) throws Exception;
	}

	private MainRunner() {
		assert false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Iterator<Main> loader = ServiceLoader.load(Main.class).iterator();

		if (args.length == 0) {
			System.out.println("Usage: " + MainRunner.class.getSimpleName() + " <command>");
			System.out.println("where <command> is one of:");
			while (loader.hasNext()) {
				MainRunner.Main main = loader.next();
				System.out.println(main.getClass().getSimpleName());
			}
			System.exit(-1);
		}

		String requestedCommand = args[0];
		String[] params = new String[args.length - 1];
		System.arraycopy(args, 1, params, 0, params.length);
		while (loader.hasNext()) {
			MainRunner.Main main = loader.next();
			if (main.getClass().getSimpleName().equalsIgnoreCase(requestedCommand)) {
				main._main(params);
				break;
			}
		}
	}

}
