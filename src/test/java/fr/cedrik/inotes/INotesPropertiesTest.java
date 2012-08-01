/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author C&eacute;drik LIME
 */
public class INotesPropertiesTest {
	private INotesProperties props;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		props = new INotesProperties();
	}

	@After
	public void tearDown() throws Exception {
		props = null;
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.INotesProperties#getServerAddress()}.
	 */
	@Test
	public void testGetServerAddress() {
		assertNotNull(props.getServerAddress());
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.INotesProperties#getUserName()}.
	 */
	@Test
	public void testGetUserName() {
		assertNotNull(props.getUserName());
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.INotesProperties#getUserPassword()}.
	 */
	@Test
	public void testGetUserPassword() {
		assertNotNull(props.getUserPassword());
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.INotesProperties#getProxy()}.
	 */
	@Test
	public void testGetProxy() {
		assertNotNull(props.getProxy());
	}

}
