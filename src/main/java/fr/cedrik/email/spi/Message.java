/**
 *
 */
package fr.cedrik.email.spi;

import java.util.Date;

/**
 * @author C&eacute;drik LIME
 */
public interface Message {
	String getId();
	Date getDate();
	int getSize();
}
