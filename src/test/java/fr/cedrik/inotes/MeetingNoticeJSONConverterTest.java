/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

/**
 * @author C&eacute;drik LIME
 */
public class MeetingNoticeJSONConverterTest {
	private static MeetingNoticeJSONConverter converter;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		converter = new MeetingNoticeJSONConverter();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		converter = null;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link fr.cedrik.inotes.MeetingNoticeJSONConverter#convertJSON(InputStream, java.nio.charset.Charset)}.
	 */
	@Test
	public void testConvertJSON() throws IOException, ValidationException {
		InputStream is = getClass().getResourceAsStream("/meetingNotice.js");
		Calendar ics = converter.convertJSON(is, null);
		is.close();
		assertNotNull("convertJSON", ics);
		ics.validate();
	}

}
