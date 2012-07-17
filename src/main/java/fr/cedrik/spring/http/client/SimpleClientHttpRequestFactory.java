/**
 *
 */
package fr.cedrik.spring.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.http.client.ClientHttpRequest;

/**
 * Note: does not do authentication; BASIC can be added to the returned {@link ClientHttpRequest}'s headers.
 *
 * @see "https://jira.springframework.org/browse/SPR-7743"
 * @author C&eacute;drik LIME
 */
public class SimpleClientHttpRequestFactory extends org.springframework.http.client.SimpleClientHttpRequestFactory {

	private boolean allowUserInteraction = URLConnection.getDefaultAllowUserInteraction();
	private boolean useCaches = getDefaultUseCaches();
	private long ifModifiedSince = 0;

	private static boolean getDefaultUseCaches() {
		try {
			// Doesn't matter that this JAR doesn't exist - just as long as the URL is well-formed
			// The getDefaultUseCaches() should have been static...
			// http://bugs.sun.com/view_bug.do?bug_id=4528126
			URL url = new URL("jar:file://dummy.jar!/");
			URLConnection uConn = url.openConnection();
			return uConn.getDefaultUseCaches();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException ignore) {
		}
		return true; // default value for URLConnection#defaultUseCaches
	}

	/**
	 *
	 */
	public SimpleClientHttpRequestFactory() {
		super();
	}

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		connection.setAllowUserInteraction(allowUserInteraction);
		super.prepareConnection(connection, httpMethod);
		connection.setUseCaches(useCaches);
		connection.setIfModifiedSince(ifModifiedSince);
//		connection.addRequestProperty("Authorization", BASIC_AUTH + Base64.encode("user name" + ':' + "pass phrase"));
	}

	/**
	 * @param allowUserInteraction the value of the {@code allowUserInteraction} field of this {@code URLConnection}
	 * @see java.net.URLConnection#setAllowUserInteraction(boolean)
	 */
	public void setAllowUserInteraction(boolean allowUserInteraction) {
		this.allowUserInteraction = allowUserInteraction;
	}

	/**
	 * @param useCaches a {@code boolean} indicating whether or not to allow caching
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 */
	public void setUseCaches(boolean useCaches) {
		this.useCaches = useCaches;
	}

	/**
	 * @param ifModifiedSince the value of the {@code ifModifiedSince} field of this {@code URLConnection}
	 * @see java.net.URLConnection#setIfModifiedSince(long)
	 */
	public void setIfModifiedSince(long ifModifiedSince) {
		this.ifModifiedSince = ifModifiedSince;
	}

}
