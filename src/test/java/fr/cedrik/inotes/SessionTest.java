/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.spi.Message;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class SessionTest {
	private static INotesProperties iNotes;
	private static Session session;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		iNotes = new INotesProperties(INotesProperties.FILE);
		session = new Session(iNotes);
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
		if (session.login(iNotes.getUserName(), iNotes.getUserPassword())) {
			try {
				MessagesMetaData<MessageMetaData> messages = session.getMessagesMetaData();
				assertNotNull("messages", messages);
				assertNotNull("messages.entries", messages.entries);
				if (! messages.entries.isEmpty()) {
					checkMessagesOrder(messages.entries);
					MessageMetaData message = messages.entries.get(0);
					IteratorChain<String> mimeHeaders = session.getMessageMIMEHeaders(message);
					assertNotNull("MIME headers", mimeHeaders);
					assertTrue("MIME", mimeHeaders.hasNext());
					assertTrue("Empty MIME headers", mimeHeaders.hasNext());
					mimeHeaders.close();
					IteratorChain<String> mime = session.getMessageMIME(message);
					assertNotNull("MIME", mime);
					assertTrue("MIME", mime.hasNext());
					assertTrue("Empty MIME", mime.hasNext());
					mime.close();
				}
//				INotesMessagesMetaData<MeetingNoticeMetaData> notices = session.getMeetingNoticesMetaData();
//				assertNotNull("notices", notices);
//				assertNotNull("notices.entries", notices.entries);
//				if (! notices.entries.isEmpty()) {
//					MeetingNoticeMetaData notice = notices.entries.get(0);
//					Calendar ics = session.getMeetingNoticeICS(notice);
//					assertNotNull("ICS Meeting Notice", ics);
//				}
				MessagesMetaData<? extends Message> allMessages = session.getMessagesAndMeetingNoticesMetaData();
				assertNotNull("allMessages", allMessages);
				assertNotNull("allMessages.entries", allMessages.entries);
			} finally {
				boolean logout = session.logout();
				assertTrue("logout", logout);
			}
		}
	}

	/**
	 * Check messages are in ASCending order
	 * @param messages
	 */
	protected void checkMessagesOrder(List<? extends BaseINotesMessage> messages) {
		Date date = new Date(0);
		for (BaseINotesMessage message : messages) {
			if (date.after(message.getDate())) {
				throw new IllegalArgumentException(message.toString());
			}
			date = message.getDate();
		}
	}
}
