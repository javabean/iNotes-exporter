/**
 *
 */
package fr.cedrik.email.fs.maildir;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cedrik.email.FoldersList;
import fr.cedrik.email.fs.maildir.MailDirPP;
import fr.cedrik.inotes.Folder;

/**
 * @author C&eacute;drik LIME
 */
public class MailDirPPTest {
	private FoldersList folders;
	private MailDirPP maildirpp;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		maildirpp = new MailDirPP();
		maildirpp.baseMailDir = new File("/");
		folders = new FoldersList();
		folders.add(newFolder(Folder.INBOX,     "Courrier en arrivée", 0, "5"));
		folders.add(newFolder(Folder.DRAFTS,    "Brouillons", 0, "7"));
		folders.add(newFolder(Folder.SENT,      "Envoyés", 0, "8"));
		folders.add(newFolder(Folder.FOLLOW_UP, "Suivi", 0, "9"));
		folders.add(newFolder(Folder.ALL,       "Tous documents", 0, "10"));
		folders.add(newFolder("f76d6b3b15e0d1abc125799f003a9b4f", "AO", 1, "15.2"));
		folders.add(newFolder("10044f13357f575bc12579c000540c62", "2012", 2, "15.2.1"));
		folders.add(newFolder("dd25dc8e89f1cab1c12579f3004e8ab4", "B", 3, "15.2.1.1"));
		folders.add(newFolder("538854649479139ec12579fb00334769", "C", 3, "15.2.1.2"));
		folders.add(newFolder("8dac8f80509cecf6c12579c7004868bd", "E", 3, "15.2.1.3"));
	}

	@After
	public void tearDown() throws Exception {
		folders = null;
		maildirpp = null;
	}

	protected Folder newFolder(String id, String name, int levelNumber, String levelTree) {
		Folder folder = new Folder();
		folder.id = id;
		folder.name = name;
		folder.levelNumber = levelNumber;
		folder.levelTree = levelTree;
		return folder;
	}

	/**
	 * Test method for {@link fr.cedrik.email.fs.maildir.MailDirPP#computeMaildirFolderName(Folder, FoldersList)}.
	 */
	@Test
	public void testComputeMaildirFolderName() {
		Folder folder = folders.getFolderById("dd25dc8e89f1cab1c12579f3004e8ab4");
		String fullName = maildirpp.computeMaildirFolderName(folder, folders);
		assertEquals("/.AO.2012.B", fullName);
	}

}
