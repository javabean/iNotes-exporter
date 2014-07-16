/**
 *
 */
package fr.cedrik.email.fs.mbox;

import java.io.IOException;

/**
 * MMDF - Multi-channel Memorandum Distribution Facility mailbox format
 *
 * @author C&eacute;drik LIME
 */
public class MMDF extends BaseMBox {
	public static final String EXTENSION_MMDF = ".mmdf";//$NON-NLS-1$

	public MMDF() throws IOException {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new MMDF().run(args, EXTENSION_MMDF);
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
		this.writer = new MMDFWriter(baseName, extension);
		return super.prepareDestinationObjects(baseName, extension);
	}

}
