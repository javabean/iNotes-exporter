/**
 *
 */
package fr.cedrik.inotes.fs.mbox;

import java.io.IOException;

/**
 * @author C&eacute;drik LIME
 * @deprecated This format can loose information. Use {@link MBoxrd} instead.
 */
@Deprecated
public class MBoxo extends BaseMBox {
	public static final String EXTENSION_MBOXO = ".mboxo";//$NON-NLS-1$

	public MBoxo() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MBoxo().run(args, EXTENSION_MBOXO);
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
		this.writer = new MBoxoWriter(baseName, extension);
		return super.prepareDestinationObjects(baseName, extension);
	}
}
