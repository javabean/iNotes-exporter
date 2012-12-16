/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author C&eacute;drik LIME
 */
public class FoldersListTest {
	private FoldersList folders;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		folders = new FoldersList();
		folders.add(newFolder(Folder.INBOX,     "Courrier en arrivée", 0, "5"));
		folders.add(newFolder(Folder.DRAFTS,    "Brouillons", 0, "7"));
		folders.add(newFolder(Folder.SENT,      "Envoyés", 0, "8"));
		folders.add(newFolder(Folder.FOLLOW_UP, "Suivi", 0, "9"));
		folders.add(newFolder(Folder.ALL,       "Tous documents", 0, "10"));
		folders.add(newFolder("d8d6cf6690ebd104c12579a30031a548", "Archives", 1, "15.2"));
		folders.add(newFolder("ffd17d9ac6bd6562c12579a30031a954", "2011", 2, "15.2.1"));
		folders.add(newFolder("ab71fccc3fc4ea89c12579a3004a28de", "2012", 2, "15.2.2"));
		folders.add(newFolder("836830748714d965c12579a30031c830", "Clients", 1, "15.3"));
		folders.add(newFolder("49ea3e6e51c0069bc1257a390049dd75", "B",  2, "15.3.1"));
		folders.add(newFolder("91039e5369936aa2c12579a30031cfb6", "N",  2, "15.3.2"));
		folders.add(newFolder("521eda9bfc78277bc12579e900366124", "BC", 3, "15.3.2.1"));
		folders.add(newFolder("1d21da66bd0819ddc12579a5004abc64", "H",  3, "15.3.2.2"));
		folders.add(newFolder("d5ab55f603d96fabc12579ad003275f6", "B",  4, "15.3.2.2.1"));
		folders.add(newFolder("a6b8339296d84966c12579b80046db07", "C",  4, "15.3.2.2.2"));
		folders.add(newFolder("3ebf09e0ed2d77acc12579b80046e72c", "L",  4, "15.3.2.2.3"));
		folders.add(newFolder("2d88f69c6b89b047c12579a5004ac10c", "P",  4, "15.3.2.2.4"));
		folders.add(newFolder("036a2778207837cfc12579bb00344fe3", "T",  3, "15.3.2.3"));
		folders.add(newFolder("9d02b28ebc2d741bc12579c3006981c6", "UC", 3, "15.3.2.4"));
		folders.add(newFolder("ee09276d35e84952c12579a400363f42", "YP", 3, "15.3.2.5"));
	}

	@After
	public void tearDown() throws Exception {
		folders = null;
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
	 * Test method for {@link fr.cedrik.inotes.FoldersList#getInbox()}.
	 */
	@Test
	public void testGetInbox() {
		Folder folder = folders.getInbox();
		assertNotNull("getInbox()", folder);
		assertSame(folder, folders.getInbox());
		assertSame(folder, folders.getFolderById(Folder.INBOX));
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.FoldersList#getFolderById(java.lang.String)}.
	 */
	@Test
	public void testGetFolderById() {
		assertSame(folders.getInbox(), folders.getFolderById(Folder.INBOX));
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.FoldersList#getFolderByLevelTree(java.lang.String)}.
	 */
	@Test
	public void testGetFolderByLevelTree() {
		//folders.add(newFolder("521eda9bfc78277bc12579e900366124", "BC", 3, "15.3.2.1"));
		Folder folder = folders.getFolderByLevelTree("15.3.2.1");
		assertEquals("id", "521eda9bfc78277bc12579e900366124", folder.id);
		assertEquals("name", "BC", folder.name);
		assertEquals("levelNumber", 3, folder.levelNumber);
		assertEquals("levelTree", "15.3.2.1", folder.levelTree);
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.FoldersList#getParent(fr.cedrik.inotes.Folder)}.
	 */
	@Test
	public void testGetParent() {
		Folder child = folders.getFolderById("91039e5369936aa2c12579a30031cfb6");
		Folder parent = folders.getFolderById("836830748714d965c12579a30031c830");
		assertSame(parent, folders.getParent(child));
		Folder root = folders.getInbox();
		assertNull("Inbox", folders.getParent(root));
		root = folders.getFolderById("d8d6cf6690ebd104c12579a30031a548");
		assertNull("Archives", folders.getParent(root));
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.FoldersList#getFoldersChain(fr.cedrik.inotes.Folder)}.
	 */
	@Test
	public void testGetFoldersChain() {
		Folder folder;
		List<Folder> chain;
		folder = folders.getInbox();
		chain = folders.getFoldersChain(folder);
		assertNotNull("Inbox", chain);
		assertTrue("Inbox.size", chain.size() == 1);
		folder = folders.getFolderById("d8d6cf6690ebd104c12579a30031a548");
		chain = folders.getFoldersChain(folder);
		assertNotNull("Archives", chain);
		assertTrue("Archives.size", chain.size() == 1);
		folder = folders.getFolderById("ab71fccc3fc4ea89c12579a3004a28de");
		chain = folders.getFoldersChain(folder);
		assertNotNull("2012", chain);
		assertTrue("2012.size", chain.size() == 2);
	}

}
