/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.IOException;

/**
 * @author C&eacute;drik LIME
 */
public class MBoxrd extends BaseMBox {
	public static final String EXTENSION_MBOXRD = ".mboxrd";//$NON-NLS-1$

	public MBoxrd() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MBoxrd().run(args, EXTENSION_MBOXRD);
	}

	/**
	 * @param args
	 */
	@Override
	public void _main(String[] args) throws IOException {
		main(args);
	}

	@Override
	protected boolean prepareDestinationObjects(String baseName, String extension) {
		this.writer = new MBoxrdWriter(baseName, extension);
		return super.prepareDestinationObjects(baseName, extension);
	}
}
