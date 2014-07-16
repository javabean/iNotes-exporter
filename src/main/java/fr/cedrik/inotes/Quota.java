/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cedrik.email.MessagesMetaData;

/**
 * @author C&eacute;drik LIME
 */
public class Quota implements fr.cedrik.email.MainRunner.Main {
	private static final Logger logger = LoggerFactory.getLogger(Quota.class);

	public Quota() {
	}

	/**
	 * @param args
	 */
	@Override
	public void _main(String[] args) throws IOException {
		main(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		INotesProperties iNotes = new INotesProperties(INotesProperties.FILE);
		Session session = new Session(iNotes);
		// login
		if (! session.login(iNotes.getUserName(), iNotes.getUserPassword())) {
			logger.error("Can not login user {}!", iNotes.getUserName());
			return;
		}
		try {
			MessagesMetaData<?> messages = session.getMessagesMetaData(0);
			/*
			<dbquotasize>
				<dbsize>121938</dbsize>
				<sizelimit>1280000</sizelimit>
				<warning>768000</warning>
				<ignorequota>0</ignorequota>
				<currentusage>121938</currentusage>
			</dbquotasize>
			 */
			if (messages.ignorequota == 0 && messages.sizelimit > 0) {
				if (messages.dbsize >= messages.sizelimit || messages.currentusage >= messages.sizelimit) {
					System.out.println("WARNING WARNING: you have exceeded your quota!");
				} else if (messages.dbsize > messages.warning || messages.currentusage > messages.warning) {
					System.out.println("WARNING: you are nearing your quota.");
				}
			}
			System.out.println("dbsize:\t\t" + messages.dbsize);
			System.out.println("currentusage:\t" + messages.currentusage);
			System.out.println("warning:\t" + messages.warning);
			System.out.println("sizelimit:\t" + messages.sizelimit);
			System.out.println("ignorequota:\t" + messages.ignorequota);
		} finally {
			session.logout();
		}
	}

}
