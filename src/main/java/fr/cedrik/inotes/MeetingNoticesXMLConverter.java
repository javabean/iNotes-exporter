/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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

/**
 * @author C&eacute;drik LIME
 */
// StAX event API
class MeetingNoticesXMLConverter {
	protected static final Logger logger = LoggerFactory.getLogger(MeetingNoticesXMLConverter.class);

	public MeetingNoticesXMLConverter() {
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

	public INotesMessagesMetaData<MeetingNoticeMetaData> convertXML(InputStream input, Charset charset) throws IOException, XMLStreamException {
		XMLEventReader reader = getXMLEventReader(input, charset);

		INotesMessagesMetaData<MeetingNoticeMetaData> notices = new INotesMessagesMetaData<MeetingNoticeMetaData>();
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				StartElement start = next.asStartElement();
				String startName = start.getName().getLocalPart();
				if ("viewentry".equals(startName)) {
					try {
						MeetingNoticeMetaData notice = new MeetingNoticeMetaData();
						loadViewEntry(notice, start, reader);
						if (StringUtils.isBlank(notice.unid)) {
							logger.error("Error while parsing XML viewentry: empty unid!");
							return null;
						}
						notices.entries.add(notice);
					} catch (ParseException e) {
						logger.error("", e);
						return null;
					}
				} else if ("dbquotasize".equals(startName)) {
					loadDbQuotaSize(notices, reader);
				} else {
					logger.debug("Unknown root element: {}", startName);
				}
			}
		}
		reader.close();
		return notices;
	}

	protected void loadDbQuotaSize(INotesMessagesMetaData<?> notices, XMLEventReader reader) throws XMLStreamException {
		while (reader.hasNext()) {
			XMLEvent next = reader.nextEvent();
			if (next.isStartElement()) {
				StartElement start = next.asStartElement();
				String startName = start.getName().getLocalPart();
				if ("dbsize".equals(startName)) {
					notices.dbsize = Integer.parseInt(readElementValue(reader, start));
				} else if ("sizelimit".equals(startName)) {
					notices.sizelimit = Integer.parseInt(readElementValue(reader, start));
				} else if ("warning".equals(startName)) {
					notices.warning = Integer.parseInt(readElementValue(reader, start));
				} else if ("ignorequota".equals(startName)) {
					notices.ignorequota = Integer.parseInt(readElementValue(reader, start));
				} else if ("currentusage".equals(startName)) {
					notices.currentusage = Integer.parseInt(readElementValue(reader, start));
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

	protected void loadViewEntry(MeetingNoticeMetaData notice, StartElement viewEntry, XMLEventReader reader) throws XMLStreamException, ParseException {
		Attribute unid = viewEntry.getAttributeByName(QName.valueOf("unid"));
		notice.unid = unid.getValue();
		Attribute noteid = viewEntry.getAttributeByName(QName.valueOf("noteid"));
		notice.noteid = noteid.getValue();

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
//					if ("$149".equals(name)) {
//						message.xxx = value;
					if ("$144".equals(name)) { // meeting date
						if (StringUtils.isNotBlank(value)) {
							notice.meetingDate = new SimpleDateFormat("yyyyMMdd'T'HHmmss','SS'Z'").parse(value);
						}
//					} else if ("SametimeInfo".equals(name)) { // availability
//						message.availability = value;
					} else if ("$2".equals(name)) { // from
						notice.from = value;
					} else if ("$3".equals(name)) { // date
						if (StringUtils.isNotBlank(value)) {
							notice.date = new SimpleDateFormat("yyyyMMdd'T'HHmmss','SS'Z'").parse(value);
						}
					} else if ("$147".equals(name)) { // subject
						notice.subject = value;
//					} else if ("$146".equals(name)) {
//						message.xxx = value;
//					} else if ("$154".equals(name)) {
//						message.xxx = value;
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
