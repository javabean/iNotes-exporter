/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.util.DateUtils;

/**
 * @author C&eacute;drik LIME
 */
// StAX event API
class MessagesXMLConverter {
	protected static final Logger logger = LoggerFactory.getLogger(MessagesXMLConverter.class);

	public MessagesXMLConverter() {
	}

	protected XMLEventReader getXMLEventReader(InputStream input, Charset charset)
			throws FactoryConfigurationError, XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		if (factory.isPropertySupported(XMLInputFactory.IS_VALIDATING)) {
			factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
		}
		if (factory.isPropertySupported(XMLInputFactory.IS_COALESCING)) {
			factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		}
		factory.setXMLReporter(new XMLReporter() {
			@Override
			public void report(String message, String errorType,
					Object relatedInformation, Location location)
					throws XMLStreamException {
				logger.warn("XML error of type: " + errorType + " at ligne: " + location.getLineNumber() + " column: " + location.getColumnNumber() + ", message: " + message);
			}
		});
		XMLEventReader reader;
		if (charset != null) {
			reader = factory.createXMLEventReader(input, charset.name());
		} else {
			reader = factory.createXMLEventReader(input);
		}
		return reader;
	}

	public MessagesMetaData<MessageMetaData> convertXML(InputStream input, Charset charset) throws IOException, XMLStreamException {
		XMLEventReader reader = getXMLEventReader(input, charset);

		MessagesMetaData<MessageMetaData> messages = new MessagesMetaData<MessageMetaData>();
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				StartElement start = next.asStartElement();
				String startName = start.getName().getLocalPart();
				if ("viewentry".equals(startName)) {
					try {
						MessageMetaData message = new MessageMetaData();
						loadViewEntry(message, start, reader);
						if (StringUtils.isBlank(message.getId())) {
							logger.error("Error while parsing XML viewentry: empty unid!");
							return null;
						}
						messages.entries.add(message);
					} catch (ParseException e) {
						logger.error("", e);
						return null;
					}
				} else if ("dbquotasize".equals(startName)) {
					loadDbQuotaSize(messages, reader);
				} else if ("unreadinfo".equals(startName)) {
					loadUnreadInfo(messages, reader);
				} else {
					logger.debug("Unknown root element: {}", startName);
				}
			}
		}
		reader.close();
		return messages;
	}

	protected void loadDbQuotaSize(MessagesMetaData<?> messages, XMLEventReader reader) throws XMLStreamException {
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				StartElement start = next.asStartElement();
				String startName = start.getName().getLocalPart();
				if ("dbsize".equals(startName)) {
					messages.dbsize = Integer.parseInt(readElementValue(reader, start));
				} else if ("sizelimit".equals(startName)) {
					messages.sizelimit = Integer.parseInt(readElementValue(reader, start));
				} else if ("warning".equals(startName)) {
					messages.warning = Integer.parseInt(readElementValue(reader, start));
				} else if ("ignorequota".equals(startName)) {
					messages.ignorequota = Integer.parseInt(readElementValue(reader, start));
				} else if ("currentusage".equals(startName)) {
					messages.currentusage = Integer.parseInt(readElementValue(reader, start));
				} else {
					logger.debug("Unknown DbQuotaSize element: {}", startName);
				}
			} else if (next.isEndElement()) {
				if ("dbquotasize".equals(next.asEndElement().getName().getLocalPart())) {
					return;
				}
			}
		}
	}

	protected void loadUnreadInfo(MessagesMetaData<?> messages, XMLEventReader reader) throws XMLStreamException {
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				StartElement start = next.asStartElement();
				String startName = start.getName().getLocalPart();
				if ("foldername".equals(startName)) {
					messages.foldername = readElementValue(reader, start);
				} else if ("unreadcount".equals(startName)) {
					try {
						messages.unreadcount = Integer.parseInt(readElementValue(reader, start));
					} catch (NumberFormatException ignore) {
						logger.trace(ignore.toString());
					}
				} else {
					logger.debug("Unknown UnreadInfo element: {}", startName);
				}
			} else if (next.isEndElement()) {
				if ("unreadinfo".equals(next.asEndElement().getName().getLocalPart())) {
					return;
				}
			}
		}
	}

	protected void loadViewEntry(MessageMetaData message, StartElement viewEntry, XMLEventReader reader) throws XMLStreamException, ParseException {
		Attribute unid = viewEntry.getAttributeByName(QName.valueOf("unid"));
		message.unid = unid.getValue();
		Attribute noteid = viewEntry.getAttributeByName(QName.valueOf("noteid"));
		message.noteid = noteid.getValue();
		Attribute unread = viewEntry.getAttributeByName(QName.valueOf("unread"));
		if (unread != null) {
			message.unread = Boolean.parseBoolean(unread.getValue());
		}

		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				StartElement start = next.asStartElement();
				String startName = start.getName().getLocalPart();
				if ("entrydata".equals(startName)) {
					// read data value
					String value = readSingleSubElementValue(reader, start);
					// determine which attribute this is
					String name = start.getAttributeByName(QName.valueOf("name")).getValue();
					if ("$86".equals(name)) { // type
						if (StringUtils.isNotBlank(value)) {
							message.type = Integer.parseInt(value);
						}
					} else if ("$Importance".equals(name)) { // importance
						if (StringUtils.isNotBlank(value)) {
							message.importance = Integer.parseInt(value);
						}
//					} else if ("SametimeInfo".equals(name)) { // availability
//						message.availability = value;
					} else if ("$93".equals(name)) { // from
						message.from93 = value;
					} else if ("$98".equals(name)) { // from
						message.from98 = value;
//					} else if ("$ThreadColumn".equals(name)) { // thread
//						message.thread = value;
					} else if ("$73".equals(name)) { // subject
						message.subject = value;
					} else if ("$70".equals(name)) { // date
						if (StringUtils.isNotBlank(value)) {
							message.date = DateUtils.parseLotusXMLDate(value);
						}
					} else if ("$106".equals(name)) { // size
						if (StringUtils.isNotBlank(value)) {
							message.size = Integer.parseInt(value);
						}
					} else if ("$ToStuff".equals(name)) { // recipient
						if (StringUtils.isNotBlank(value)) {
							message.recipient = Integer.parseInt(value);
						}
					} else if ("$97".equals(name)) { // attachement
						if (StringUtils.isNotBlank(value)) {
							message.attachement = Integer.parseInt(value);
						}
					} else if ("$109".equals(name)) { // answer flag
						if (StringUtils.isNotBlank(value)) {
							message.answerFlag = Integer.parseInt(value);
						}
//					} else if ("$UserData".equals(name)) { // user data
//						message.userData = value;
					} else {
						logger.debug("Unknown EntryData attribute value: {}", name);
					}
				} else {
					logger.debug("Unknown ViewEntry element: {}", startName);
				}
			} else if (next.isEndElement()) {
				if ("viewentry".equals(next.asEndElement().getName().getLocalPart())) {
					return;
				}
			}
		}
	}

	protected String readSingleSubElementValue(XMLEventReader reader, StartElement start) throws XMLStreamException {
		String value = null;
		boolean inValueElement = false;
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				inValueElement = true;
				StartElement startData = next.asStartElement();
				logger.trace("{} element: {}", start.getName().getLocalPart(), startData.getName().getLocalPart());
			} else if (next.isCharacters() && inValueElement) {
				value = next.asCharacters().getData();
			} else if (next.isEndElement()) {
				inValueElement = false;
				if (start.getName().getLocalPart().equals(next.asEndElement().getName().getLocalPart())) {
					return value;
				}
			}
		}
		return value;
	}

	protected String readElementValue(XMLEventReader reader, StartElement start) throws XMLStreamException {
		String value = null;
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isCharacters()) {
				value = next.asCharacters().getData();
			} else if (next.isEndElement()) {
				if (start.getName().getLocalPart().equals(next.asEndElement().getName().getLocalPart())) {
					return value;
				}
			}
		}
		return value;
	}
}
