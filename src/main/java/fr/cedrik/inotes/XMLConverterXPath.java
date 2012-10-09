/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * WARNING: this class is not thread-safe!
 *
 * @author C&eacute;drik LIME
 * @deprecated XPath is simply too slow…
 */
@Deprecated
class XMLConverterXPath {
	protected static final Logger logger = LoggerFactory.getLogger(XMLConverterXPath.class);

	public XMLConverterXPath() {
	}

	protected Document loadXML(InputStream input) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(input);
		doc.getDocumentElement().normalize();
		return doc;
	}

	public MessagesMetaData convertXML(InputStream input) throws IOException {
		Document doc;
		try {
			doc = loadXML(input);
		} catch (ParserConfigurationException e) {
			logger.error("", e);
			return null;
		} catch (SAXException e) {
			logger.error("", e);
			return null;
		}
		MessagesMetaData messages = new MessagesMetaData();
		try {
			Number dbsize       = (Number) dbsizeXP.evaluate(doc, XPathConstants.NUMBER);
			Number sizelimit    = (Number) sizelimitXP.evaluate(doc, XPathConstants.NUMBER);
			Number warning      = (Number) warningXP.evaluate(doc, XPathConstants.NUMBER);
			Number ignorequota  = (Number) ignorequotaXP.evaluate(doc, XPathConstants.NUMBER);
			Number currentusage = (Number) currentusageXP.evaluate(doc, XPathConstants.NUMBER);
			String foldername   = (String) foldernameXP.evaluate(doc, XPathConstants.STRING);
			Number unreadcount  = (Number) unreadcountXP.evaluate(doc, XPathConstants.NUMBER);
			messages.dbsize       = dbsize.intValue();
			messages.sizelimit    = sizelimit.intValue();
			messages.warning      = warning.intValue();
			messages.ignorequota  = ignorequota.intValue();
			messages.currentusage = currentusage.intValue();
			messages.foldername   = foldername;
			messages.unreadcount  = unreadcount.intValue();
			NodeList messagesList = (NodeList) viewentryXP.evaluate(doc, XPathConstants.NODESET);
			if (messagesList != null) {
				for (int i = 0; i < messagesList.getLength(); ++i) {
					Node node = messagesList.item(i);
					MessageMetaData message = new MessageMetaData();
					message.unid        = (String) unidXP.evaluate(node, XPathConstants.STRING);
					message.noteid      = (String) noteidXP.evaluate(node, XPathConstants.STRING);
					message.unread      = ((Boolean) unreadXP.evaluate(node, XPathConstants.BOOLEAN)).booleanValue();
					message.type        = ((Number) typeXP.evaluate(node, XPathConstants.NUMBER)).intValue();
					message.importance  = ((Number) importanceXP.evaluate(node, XPathConstants.NUMBER)).intValue();
					message.from93      = (String) from93XP.evaluate(node, XPathConstants.STRING);
					message.from98      = (String) from98XP.evaluate(node, XPathConstants.STRING);
					message.subject     = (String) subjectXP.evaluate(node, XPathConstants.STRING);
					String dateStr = (String) dateXP.evaluate(node, XPathConstants.STRING);
					if (StringUtils.isNotEmpty(dateStr)) {
						message.date        = new SimpleDateFormat("yyyyMMdd'T'HHmmss','SS'Z'").parse(dateStr);
					}
					message.size        = ((Number) sizeXP.evaluate(node, XPathConstants.NUMBER)).intValue();
					message.recipient   = ((Number) recipientXP.evaluate(node, XPathConstants.NUMBER)).intValue();
					message.attachement = ((Number) attachementXP.evaluate(node, XPathConstants.NUMBER)).intValue();
					message.answerFlag  = ((Number) answerFlagXP.evaluate(node, XPathConstants.NUMBER)).intValue();
					if (StringUtils.isBlank(message.unid)) {
						logger.error("Error while parsing XML viewentry: empty unid! {}", node);
						continue;
					}
					messages.entries.add(message);
				}
			}
		} catch (XPathExpressionException e) {
			logger.error("", e);
			return null;
		} catch (ParseException e) {
			logger.error("", e);
			return null;
		}
		return messages;
	}

	private static final XPathFactory xpFactory = XPathFactory.newInstance();
	private static final XPath xpath = xpFactory.newXPath();
	private static final XPathExpression viewentryXP;
	private static final XPathExpression dbsizeXP;
	private static final XPathExpression sizelimitXP;
	private static final XPathExpression warningXP;
	private static final XPathExpression ignorequotaXP;
	private static final XPathExpression currentusageXP;
	private static final XPathExpression foldernameXP;
	private static final XPathExpression unreadcountXP;
	private static final XPathExpression unidXP;
	private static final XPathExpression noteidXP;
	private static final XPathExpression unreadXP;
	private static final XPathExpression typeXP;
	private static final XPathExpression importanceXP;
//	private static final XPathExpression availabilityXP;
	private static final XPathExpression from93XP;
	private static final XPathExpression from98XP;
//	private static final XPathExpression threadXP;
	private static final XPathExpression subjectXP;
	private static final XPathExpression dateXP;
	private static final XPathExpression sizeXP;
	private static final XPathExpression recipientXP;
	private static final XPathExpression attachementXP;
	private static final XPathExpression answerFlagXP;
//	private static final XPathExpression userDataXP;

	static {
//		System.setProperty("com.sun.org.apache.xml.internal.dtm.DTMManager", "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault");
//		System.setProperty("org.apache.xml.dtm.DTMManager", "org.apache.xml.dtm.ref.DTMManagerDefault");

		try {
			dbsizeXP       = xpath.compile("/readviewentries/dbquotasize/dbsize");
			sizelimitXP    = xpath.compile("/readviewentries/dbquotasize/sizelimit");
			warningXP      = xpath.compile("/readviewentries/dbquotasize/warning");
			ignorequotaXP  = xpath.compile("/readviewentries/dbquotasize/ignorequota");
			currentusageXP = xpath.compile("/readviewentries/dbquotasize/currentusage");
			foldernameXP  = xpath.compile("/readviewentries/unreadinfo/foldername");
			unreadcountXP = xpath.compile("/readviewentries/unreadinfo/unreadcount");
			viewentryXP = xpath.compile("/readviewentries/viewentries/viewentry");
			unidXP        = xpath.compile("@unid");
			noteidXP      = xpath.compile("@noteid");
			unreadXP      = xpath.compile("@unread");
			typeXP        = xpath.compile("entrydata[@name='$86']/number");
			importanceXP  = xpath.compile("entrydata[@name='$Importance']/number");
//			availabilityXP = xpath.compile("entrydata[@name='SametimeInfo']/XXXXX");
			from93XP      = xpath.compile("entrydata[@name='$93']/text");
			from98XP      = xpath.compile("entrydata[@name='$98']/text");
//			threadXP      = xpath.compile("entrydata[@name='$ThreadColumn']/XXXXX");
			subjectXP     = xpath.compile("entrydata[@name='$73']/text");
			dateXP        = xpath.compile("entrydata[@name='$70']/datetime");
			sizeXP        = xpath.compile("entrydata[@name='$106']/number");
			recipientXP   = xpath.compile("entrydata[@name='$ToStuff']/number");
			attachementXP = xpath.compile("entrydata[@name='$97']/number");
			answerFlagXP  = xpath.compile("entrydata[@name='$109']/number");
//			userDataXP    = xpath.compile("entrydata[@name='$UserData']/XXXXX");
		} catch (XPathExpressionException e) {
			logger.error("", e);
			throw new RuntimeException(e);
		}
	}

}
