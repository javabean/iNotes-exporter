/**
 *
 */
package fr.cedrik.email.spi;

/**
 * @author C&eacute;drik LIME
 */
public interface Folder {
	String getId();
	String getName();
	boolean isInbox();
	boolean isAllMails();
}
