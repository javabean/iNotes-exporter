/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import fr.cedrik.inotes.util.CookieManager;
import fr.cedrik.spring.http.client.HttpRequestExecutor;
import fr.cedrik.spring.http.client.SimpleClientHttpRequestFactory;

/**
 * @author C&eacute;drik LIME
 */
public class HttpContext {
	protected final INotesProperties iNotes;
	protected final HttpRequestExecutor httpRequestExecutor;
	protected final CookieManager cookieManager = new CookieManager();
	protected final HttpHeaders httpHeaders = new HttpHeaders();
	protected String proxyBaseURL;
	protected String folderBaseURL;
	protected String mailEditBaseURL;

	static {
//		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		// Note: the following line means this application must be single-user only!
		/*
		 * Non-Oracle JVM (i.e. OpenJDK 6) do not implement {@link java.net.CookieManager} correctly.
		 * And anyway, having a JVM singleton ({@link CookieHandler#setDefault(java.net.CookieHandler)}) for cookies management sucks.
		 * Thus we need to manage cookies manually...
		 */
		//CookieHandler.setDefault(cookieManager);
	}

	public HttpContext(INotesProperties iNotes) {
		this.iNotes = iNotes;
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		httpRequestExecutor = getHttpRequestExecutor();
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

	public String getNotesFolderId() {
		return iNotes.getNotesFolderId();
	}
	public void setNotesFolderId(String notesFolderID) {
		iNotes.setNotesFolderId(notesFolderID);
	}

	public List<String> getNotesExcludedFoldersIds() {
		return iNotes.getNotesExcludedFoldersIds();
	}

	public String getProxyBaseURL() {
		return proxyBaseURL;
	}

	public void setProxyBaseURL(String baseURL) {
		this.proxyBaseURL = baseURL;
	}

	public String getFolderBaseURL() {
		return folderBaseURL + getNotesFolderId() + '/';
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

	public boolean isFixLotusNotesDateMIMEHeader() {
		return iNotes.isFixLotusNotesDateMIMEHeader();
	}

	/**
	 * @see HttpRequestExecutor#createRequest(URL, HttpMethod, Map, Map)
	 */
	public ClientHttpRequest createRequest(URL url, HttpMethod method, Map<String, ?> extraParameters) throws IOException {
		// put back cookies in httpHeaders
		try {
			Map<String, List<String>> cookies = cookieManager.get(url.toURI(), Collections.<String,List<String>>emptyMap());
			httpHeaders.putAll(cookies);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		// create http request
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

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	/**
	 * Non-Oracle JVM (i.e. OpenJDK 6) do not implement {@link java.net.CookieManager} correctly.
	 * And anyway, having a JVM singleton ({@link CookieHandler#setDefault(java.net.CookieHandler)}) for cookies management sucks.
	 * Thus we need to manage cookies manually...
	 * @param httpResponse
	 */
	public void rememberCookies(ClientHttpRequest httpRequest, ClientHttpResponse httpResponse) throws IOException {
		cookieManager.put(httpRequest.getURI(), httpResponse.getHeaders());
	}
}
