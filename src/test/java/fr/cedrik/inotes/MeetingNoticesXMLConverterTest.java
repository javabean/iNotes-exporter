/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author C&eacute;drik LIME
 */
public class MeetingNoticesXMLConverterTest {
	private static MeetingNoticesXMLConverter xmlConverter;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		xmlConverter = new MeetingNoticesXMLConverter();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		xmlConverter = null;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.MeetingNoticesXMLConverter#convertXML(InputStream, java.nio.charset.Charset)}.
	 */
	@Test
	public void testConvertXML() throws IOException, XMLStreamException {
		InputStream is = getClass().getResourceAsStream("/meetingNotices.xml");
		MeetingNoticesMetaData messages = xmlConverter.convertXML(is, null);
		is.close();
		assertNotNull("convertXML", messages);
		for (MeetingNoticeMetaData message : messages.entries) {
			assertNotNull("unid", message.unid);
			assertNotNull("noteid", message.noteid);
			assertNotNull("meetingDate", message.meetingDate);
		}
	}

}
