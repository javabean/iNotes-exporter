/**
 *
 */
package fr.cedrik.email.spi;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import fr.cedrik.email.FoldersList;
import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public interface Session {
	String getServerAddress();
	void setServerAddress(URL url);
	boolean login(String username, String password) throws IOException;
	boolean logout() throws IOException;
	FoldersList getFolders() throws IOException;
	void setCurrentFolder(Folder folder) throws IOException;
	MessagesMetaData<? extends Message> getMessagesMetaData() throws IOException;
	MessagesMetaData<? extends Message> getMessagesMetaData(int limit) throws IOException;
	MessagesMetaData<? extends Message> getMessagesMetaData(Date oldestMessageToFetch) throws IOException;
	MessagesMetaData<? extends Message> getMessagesMetaData(Date oldestMessageToFetch, Date newestMessageToFetch) throws IOException;
	Iterator<String> getMessageMIMEHeaders(Message message) throws IOException;
	IteratorChain<String> getMessageMIME(Message message) throws IOException;
	void deleteMessage(Message... messages) throws IOException;
	void deleteMessage(Collection<? extends Message> messages) throws IOException;
	void undeleteAllMessages() throws IOException;
}
