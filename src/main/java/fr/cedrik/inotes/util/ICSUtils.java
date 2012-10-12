/**
 *
 */
package fr.cedrik.inotes.util;


/**
 * @author C&eacute;drik LIME
 */
public class ICSUtils {

	private ICSUtils() {
		assert false;
	}

//	public static Part toEMail(Calendar calendar) throws MessagingException {
//		MimeMessage msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()));
//		//TODO
//		msg.setFrom(new InternetAddress(null));
//		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(null));
//		msg.setSubject(null);
//		msg.setSentDate(null);
//		// message part
//		MimeBodyPart messageBodyPart = new MimeBodyPart();
//		messageBodyPart.setText("Hi");
//		Multipart multipart = new MimeMultipart();
//		multipart.addBodyPart(messageBodyPart);
//		// attachement
//		messageBodyPart = new MimeBodyPart();
//		DataSource source = new FileDataSource((String)null);
//		messageBodyPart.setDataHandler(new DataHandler(source));
//		messageBodyPart.setFileName("invite.ics");
////		messageBodyPart.setContent(null, "text/calendar");
//		multipart.addBodyPart(messageBodyPart);
//
//		msg.setContent(multipart);
//		msg.saveChanges();
//		return msg;
//	}
}
