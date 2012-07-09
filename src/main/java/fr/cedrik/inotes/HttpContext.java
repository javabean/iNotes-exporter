/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import fr.cedrik.spring.http.client.HttpRequestExecutor;
import fr.cedrik.spring.http.client.SimpleClientHttpRequestFactory;

/**
 * @author C&eacute;drik LIME
 */
public class HttpContext {
	protected static final CookieManager cookieManager = new CookieManager();

	protected final INotesProperties iNotes = new INotesProperties();
	protected final HttpRequestExecutor httpRequestExecutor = getHttpRequestExecutor();
	protected final Map<String, String> httpHeaders = new HashMap<String, String>();
	protected String proxyBaseURL;
	protected String folderBaseURL;
	protected String mailEditBaseURL;

	static {
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		// Note: the following line means this application must be single-user only!
		CookieHandler.setDefault(cookieManager);
	}

	public HttpContext() {
	}

	/**
	 * Alternative: use Spring! :-)
	 * @return
	 */
	private HttpRequestExecutor getHttpRequestExecutor() {
		HttpRequestExecutor http = new HttpRequestExecutor();
		{
			SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
			Proxy proxy = iNotes.getProxy();
			httpRequestFactory.setProxy(proxy);
			httpRequestFactory.setAllowUserInteraction(false);
			httpRequestFactory.setUseCaches(false);
			httpRequestFactory.setConnectTimeout(10000);
			httpRequestFactory.setReadTimeout(30000);
			http.setRequestFactory(httpRequestFactory);
		}
		return http;
	}

	public String getServerAddress() {
		return iNotes.getServerAddress();
	}

	public String getUserName() {
		return iNotes.getUserName();
	}
	public void setUserName(String userName) {
		iNotes.setUserName(userName);
	}

	public String getUserPassword() {
		return iNotes.getUserPassword();
	}
	public void setUserPassword(String password) {
		iNotes.setUserPassword(password);
	}

	public Proxy getProxy() {
		return iNotes.getProxy();
	}

	public String getNotesFolderName() {
		return iNotes.getNotesFolderName();
	}
	public void setNotesFolderName(String notesFolderID) {
		iNotes.setNotesFolderName(notesFolderID);
	}

	public String getProxyBaseURL() {
		return proxyBaseURL;
	}

	public void setProxyBaseURL(String baseURL) {
		this.proxyBaseURL = baseURL;
	}

	public String getFolderBaseURL() {
		return folderBaseURL + getNotesFolderName() + '/';
	}

	public void setFolderBaseURL(String baseURL) {
		this.folderBaseURL = baseURL;
	}

	public String getMailEditBaseURL() {
		return mailEditBaseURL;
	}

	public void setMailEditBaseURL(String baseURL) {
		this.mailEditBaseURL = baseURL;
	}

	/**
	 * @see HttpRequestExecutor#createRequest(URL, HttpMethod, Map, Map)
	 */
	public ClientHttpRequest createRequest(URL url, HttpMethod method, Map<String, ?> extraParameters) throws IOException {
		return httpRequestExecutor.createRequest(url, method, extraParameters, httpHeaders);
	}

	/**
	 * @see HttpRequestExecutor#getCharset(ClientHttpResponse)
	 */
	public Charset getCharset(ClientHttpResponse httpResponse) {
		return httpRequestExecutor.getCharset(httpResponse);
	}

	public CookieStore getCookieStore() {
		return cookieManager.getCookieStore();
	}

	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}
}
