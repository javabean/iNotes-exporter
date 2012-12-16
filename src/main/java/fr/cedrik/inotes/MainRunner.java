/**
 *
 */
package fr.cedrik.inotes;

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
		if (args.length == 0) {
			help();
			return;
		}

		String requestedCommand = args[0];
		String[] params = new String[args.length - 1];
		System.arraycopy(args, 1, params, 0, params.length);
		boolean hasRun = false;
		for (MainRunner.Main main : ServiceLoader.load(Main.class)) {
			if (main.getClass().getSimpleName().equalsIgnoreCase(requestedCommand)) {
				hasRun = true;
				main._main(params);
				break;
			}
		}

		if (! hasRun) {
			help();
			return;
		}
	}

	public static void help() {
		System.out.println("Usage: " + MainRunner.class.getSimpleName() + " <command>");
		System.out.println("where <command> is one of:");
		for (MainRunner.Main main : ServiceLoader.load(Main.class)) {
			System.out.println('\t' + main.getClass().getSimpleName());
		}
		System.exit(-1);
	}
}
