/**
 *
 */
package fr.cedrik.inotes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
public class MessagesXMLConverterTest {
	private static MessagesXMLConverter xmlConverter;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		xmlConverter = new MessagesXMLConverter();
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
	 * Test method for {@link fr.cedrik.inotes.MessagesXMLConverter#convertXML(InputStream, java.nio.charset.Charset)}.
	 */
	@Test
	public void testConvertXML() throws IOException, XMLStreamException {
		InputStream is = getClass().getResourceAsStream("/messages.xml");
		MessagesMetaData messages = xmlConverter.convertXML(is, null);
		is.close();
		assertNotNull("convertXML", messages);
		for (MessageMetaData message : messages.entries) {
			assertNotNull("unid", message.unid);
			assertNotNull("noteid", message.noteid);
			assertTrue("size", message.size >= 0);
		}
	}

}
