/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.LineIterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author C&eacute;drik LIME
 */
public class SessionTest {
	private static Session session;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		session = new Session();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		session = null;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.Session}.
	 */
	@Test
	public void testSessionWorkflow() throws IOException {
		if (session.login()) {
			MessagesMetaData messages = session.getMessagesMetaData();
			assertNotNull("data", messages);
			if (! messages.entries.isEmpty()) {
				MessageMetaData message = messages.entries.get(0);
				LineIterator mimeHeaders = session.getMessageMIMEHeaders(message);
				assertNotNull("MIME headers", mimeHeaders);
				assertTrue("Empty MIME headers", mimeHeaders.hasNext());
				mimeHeaders.close();
				LineIterator mime = session.getMessageMIME(message);
				assertNotNull("MIME", mime);
				assertTrue("Empty MIME", mime.hasNext());
				mime.close();
			}
			boolean logout = session.logout();
			assertTrue("logout", logout);
		}
	}

}
