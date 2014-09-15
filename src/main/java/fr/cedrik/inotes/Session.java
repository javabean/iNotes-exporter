/**
 *
 */
package fr.cedrik.inotes;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mail.MailParseException;

import fr.cedrik.email.FoldersList;
import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.spi.Message;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class Session implements fr.cedrik.email.spi.Session {
	private static final int META_DATA_LOAD_BATCH_SIZE = 500;

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected final HttpContext context;
	protected final Set<String> mailsToDelete = new HashSet<String>();
	protected final Set<String> mailsToMarkUnread = new HashSet<String>();
	protected final Set<String> mailsToMarkRead = new HashSet<String>();
	protected final Set<String> mailsToMarkUnreadAll = new HashSet<String>();
	protected final Set<String> mailsToMarkReadAll = new HashSet<String>();
	protected final Set<String> noticesToDelete = new HashSet<String>();
	protected MessagesMetaData<MessageMetaData> allMessagesCache = null;
	protected MessagesMetaData<MeetingNoticeMetaData> allNoticesCache = null;
	protected boolean isLoggedIn = false;
	protected FoldersList folders = new FoldersList();

	static {
//		System.setProperty("java.util.logging.config.file", "logging.properties");//XXX DEBUG
	}

	public Session(INotesProperties iNotes) {
		context = new HttpContext(iNotes);
	}

	protected void trace(ClientHttpRequest httpRequest, ClientHttpResponse httpResponse) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug(httpRequest.getMethod().toString() + ' ' + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText() + ' ' + httpRequest.getURI());
		}
	}
	private void traceBody(ClientHttpResponse httpResponse) throws IOException {
		if (logger.isTraceEnabled()) {
			String responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
			logger.trace(responseBody);
		}
	}

	@Override
	public String getServerAddress() {
		return context.iNotes.getServerAddress();
	}

	@Override
	public void setServerAddress(URL url) {
		if (isLoggedIn) {
			throw new IllegalStateException();
		}
		context.iNotes.setServerAddress(url);
	}

	@Override
	public FoldersList getFolders() {
		if (! isLoggedIn) {
			throw new IllegalStateException();
		}
		return folders;
	}

	@Override
	public void setCurrentFolder(fr.cedrik.email.spi.Folder folder) throws IOException {
		if (isLoggedIn) {
			cleanup();
		}
		context.setCurrentFolderId(folder.getId());
		allMessagesCache = null;
		allNoticesCache = null;
	}

	@Override
	public boolean login(String userName, String password) throws IOException {
		context.setUserName(userName);
		context.setUserPassword(password);
		Map<String, Object> params = new HashMap<String, Object>();
		ClientHttpRequest httpRequest;
		ClientHttpResponse httpResponse;
		String responseBody;

		// Step 1a: login (auth)
		{
			params.clear();
			params.put("%%ModDate", "0000000100000000");
			params.put("RedirectTo", "/dwaredirect.nsf");
			params.put("password", context.getUserPassword());
			params.put("username", context.getUserName());
			httpRequest = context.createRequest(new URL(context.getServerAddress() + "/names.nsf?Login"), HttpMethod.POST, params);
			httpResponse = httpRequest.execute();
			trace(httpRequest, httpResponse);
			context.rememberCookies(httpRequest, httpResponse);
			if (logger.isTraceEnabled()) {
				traceBody(httpResponse);
			}
			try {
				if (httpResponse.getStatusCode().series().equals(HttpStatus.Series.REDIRECTION)) {
					logger.debug("Initial authentication successful for user \"" + context.getUserName() + '"');
					logger.debug("Redirect: {}", httpResponse.getHeaders().getLocation());
				} else if (httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
					// body will contain "Invalid username or password was specified."
					logger.warn("ERROR while authenticating user \""+context.getUserName()+"\". Please check your parameters in " + INotesProperties.FILE);
					return false;
				} else {
					logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
					return false;
				}
			} finally {
				httpResponse.close();
			}
		}

		// Step 1b: login (iNotesSRV cookie + base url)
		{
			params.clear();
			httpRequest = context.createRequest(new URL(context.getServerAddress() + "/dwaredirect.nsf"), HttpMethod.GET, params);
			httpResponse = httpRequest.execute();
			trace(httpRequest, httpResponse);
			context.rememberCookies(httpRequest, httpResponse);
			responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
			try {
				if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
					logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
					return false;
				}
			} finally {
				httpResponse.close();
			}
			if (logger.isTraceEnabled()) {
				logger.trace(responseBody);
			}
		}
		// search for additional cookie
		{
			Pattern jsCookie = Pattern.compile("<script language=javascript>document\\.cookie='([^']+)';</script>", Pattern.CASE_INSENSITIVE);
			Matcher jsCookieMatcher = jsCookie.matcher(responseBody);
			assert jsCookieMatcher.groupCount() == 1 ; jsCookieMatcher.groupCount();
			while (jsCookieMatcher.find()) {
				String cookieStr = jsCookieMatcher.group(1);
				logger.trace("Found additional cookie: {}", cookieStr);
				List<HttpCookie> cookies = HttpCookie.parse(cookieStr);
				for (HttpCookie cookie : cookies) {
					cookie.setSecure("https".equalsIgnoreCase(httpRequest.getURI().getScheme()));
					cookie.setPath("/");
					logger.trace("Adding cookie: {}", cookie);
					context.getCookieStore().add(httpRequest.getURI(), cookie);
//					context.getHttpHeaders().add("Cookie", cookie.toString());// hack, since the previous line does not work correctly when using the JVM default CookieHandler
				}
			}
		}
		// search for redirect
		final String redirectURL;
		{
			Pattern htmlRedirect = Pattern.compile("<META HTTP-EQUIV=\"refresh\" content=\"\\d;URL=([^\"]+)\">", Pattern.CASE_INSENSITIVE);
			Matcher htmlRedirectMatcher = htmlRedirect.matcher(responseBody);
			assert htmlRedirectMatcher.groupCount() == 1 ; htmlRedirectMatcher.groupCount();
			if (htmlRedirectMatcher.find()) {
				redirectURL = htmlRedirectMatcher.group(1);
				logger.trace("Found redirect URL: {}", redirectURL);
			} else {
				logger.error("Can not find the redirect URL; aborting. Response body:\n" + responseBody);
				return false;
			}
			if (htmlRedirectMatcher.find()) {
				logger.error("Found more than 1 redirect URL; aborting. Response body:\n" + responseBody);
				return false;
			}
		}
		responseBody = null;

		// Step 1c: base URL
		{
			String baseURL = redirectURL.substring(0, redirectURL.indexOf(".nsf")+".nsf".length()) + '/';
			context.setProxyBaseURL(baseURL + "iNotes/Proxy/?OpenDocument");
			context.setFolderBaseURL(baseURL);
			context.setMailEditBaseURL(baseURL + "iNotes/Mail/?EditDocument");
			logger.trace("Proxy base URL for user \"{}\": {}", context.getUserName(), context.getProxyBaseURL());
			logger.trace("Folder base URL for user \"{}\": {}", context.getUserName(), context.getFolderBaseURL());
			logger.trace("Mail edit base URL for user \"{}\": {}", context.getUserName(), context.getMailEditBaseURL());
		}

		// Step 1d: login (ShimmerS cookie)
		{
			params.clear();
			// need to emulate a real browser, or else we get an "unknown browser" response with no possibility to continue
			context.getHttpHeaders().set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:24.0) Gecko/20100101 Firefox/24.0");
//			context.getHttpHeaders().set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_5) AppleWebKit/536.30.1 (KHTML, like Gecko) Version/6.0.5 Safari/536.30.1");
			httpRequest = context.createRequest(new URL(redirectURL), HttpMethod.GET, params);
			httpResponse = httpRequest.execute();
			trace(httpRequest, httpResponse);
			context.rememberCookies(httpRequest, httpResponse);
			responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
			// Apparently we don't need to parse the embeded JS to set the "Shimmer" cookie.
			try {
				if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
					logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
					return false;
				}
			} finally {
				httpResponse.close();
			}
			if (logger.isTraceEnabled()) {
				logger.trace(responseBody);
			}
		}

		// Step 1e: get available folders
		{
			// parse the response to get the SessionFrame URL
			String sessionFrameURL;
			{
				Pattern sessionFrame = Pattern.compile("<frame name=\"s_SessionFrame\" src=\"([^\"]+Form=l_SessionFrame[^\"]+)\".*>", Pattern.CASE_INSENSITIVE);
				Matcher sessionFrameMatcher = sessionFrame.matcher(responseBody);
				assert sessionFrameMatcher.groupCount() == 1 ; sessionFrameMatcher.groupCount();
				if (sessionFrameMatcher.find()) {
					sessionFrameURL = sessionFrameMatcher.group(1);
					if (! sessionFrameURL.toLowerCase().startsWith("http")) {
						URI uri = httpRequest.getURI();
						String uriString = uri.toString();
						sessionFrameURL = uriString.substring(0, uriString.indexOf(uri.getRawPath())) + sessionFrameURL;
					}
					logger.trace("Found l_SessionFrame URL: {}", sessionFrameURL);
				} else {
					logger.error("Can not find the l_SessionFrame URL; aborting. Response body:\n" + responseBody);
					return false;
				}
				if (sessionFrameMatcher.find()) {
					logger.error("Found more than 1 l_SessionFrame URL; aborting. Response body:\n" + responseBody);
					return false;
				}
			}

			// play the SessionFrame URL to get the SessionInfo URL
			{
				params.clear();
				httpRequest = context.createRequest(new URL(sessionFrameURL), HttpMethod.GET, params);
				httpResponse = httpRequest.execute();
				trace(httpRequest, httpResponse);
				context.rememberCookies(httpRequest, httpResponse);
				responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
				try {
					if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
						logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
						return false;
					}
				} finally {
					httpResponse.close();
				}
				if (logger.isTraceEnabled()) {
					logger.trace(responseBody);
				}
			}

			String sessionInfoURL;
			{
				Pattern sessionInfo = Pattern.compile("<script src=\"([^\"]+Form=f_SessionInfo[^\"]+)\".*>", Pattern.CASE_INSENSITIVE);
				Matcher sessionInfoMatcher = sessionInfo.matcher(responseBody);
				assert sessionInfoMatcher.groupCount() == 1 ; sessionInfoMatcher.groupCount();
				if (sessionInfoMatcher.find()) {
					sessionInfoURL = sessionInfoMatcher.group(1);
					if (! sessionInfoURL.toLowerCase().startsWith("http")) {
						URI uri = httpRequest.getURI();
						String uriString = uri.toString();
						sessionInfoURL = uriString.substring(0, uriString.indexOf(uri.getRawPath())) + sessionInfoURL;
					}
					logger.trace("Found s_SessionInfo URL: {}", sessionInfoURL);
				} else {
					logger.error("Can not find the s_SessionInfo URL; aborting. Response body:\n" + responseBody);
					return false;
				}
				if (sessionInfoMatcher.find()) {
					logger.error("Found more than 1 s_SessionInfo URL; aborting. Response body:\n" + responseBody);
					return false;
				}
			}

			// play the SessionInfo URL to parse the folders (this also contains the Domino server name)
			{
				params.clear();
				httpRequest = context.createRequest(new URL(sessionInfoURL), HttpMethod.GET, params);
				httpResponse = httpRequest.execute();
				trace(httpRequest, httpResponse);
				context.rememberCookies(httpRequest, httpResponse);
				responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
				try {
					if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
						logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
						return false;
					}
				} finally {
					httpResponse.close();
				}
				if (logger.isTraceEnabled()) {
					logger.trace(responseBody);
				}
			}

			{
				Pattern jsArray = Pattern.compile("new Array\\(\"([.0-9]*?)\",(\\d+),'(.*?)','.*?','(.*?)',\".*?\"\\)", Pattern.CASE_INSENSITIVE);
				Matcher jsArrayMatcher = jsArray.matcher(responseBody);
				assert jsArrayMatcher.groupCount() == 4 ; jsArrayMatcher.groupCount();
				List<String> excludedFoldersIds = context.getExcludedFoldersIds();
				while (jsArrayMatcher.find()) {
					String levelTree   = jsArrayMatcher.group(1);
					int levelNumber    = Integer.parseInt(jsArrayMatcher.group(2));
					String name        = StringEscapeUtils.unescapeEcmaScript(jsArrayMatcher.group(3));
					String url         = jsArrayMatcher.group(4);
					if (StringUtils.isNotEmpty(url) && url.contains(".nsf/")) {
						int startIndex = url.indexOf(".nsf/") + ".nsf/".length();
						int endIndex = url.indexOf('/', startIndex+1);
						String id = url.substring(startIndex, endIndex);
						// filter folders to exclude non-user things like "Forums", "Rules", "Stationery" etc.
						if (! (levelNumber <= 1 && excludedFoldersIds.contains(id))) {
							Folder folder = new Folder();
							folder.levelTree = levelTree;
							folder.levelNumber = levelNumber;
							folder.name = name;
							folder.id = id;
							folders.add(folder);
							logger.debug("Found iNotes folder {}", folder);
						}
					}
				}
			}
		}
		responseBody = null;

		// Step 1f: X-IBM-INOTES-NONCE header
		{
			List<HttpCookie> cookies = context.getCookieStore().getCookies();
			for (HttpCookie cookie : cookies) {
				if ("ShimmerS".equals(cookie.getName())) {
					if (context.getHttpHeaders().containsKey("X-IBM-INOTES-NONCE")) {
						logger.error("Multiple cookies \"ShimmerS\" in store; aborting.");
						return false;
					}
					String xIbmINotesNonce;
					Pattern shimmerS = Pattern.compile("&N:(\\p{XDigit}+)");
					Matcher shimmerSMatcher = shimmerS.matcher(cookie.getValue());
					assert shimmerSMatcher.groupCount() == 1 ; shimmerSMatcher.groupCount();
					if (shimmerSMatcher.find()) {
						xIbmINotesNonce = shimmerSMatcher.group(1);
						logger.trace("Found X-IBM-INOTES-NONCE: {}", xIbmINotesNonce);
					} else {
						logger.error("Can not find X-IBM-INOTES-NONCE; aborting. ShimmerS cookie: " + cookie);
						return false;
					}
					if (shimmerSMatcher.find()) {
						logger.error("Found more than 1 X-IBM-INOTES-NONCE; aborting. ShimmerS cookie: " + cookie);
						return false;
					}
					context.getHttpHeaders().set("X-IBM-INOTES-NONCE", xIbmINotesNonce);
				}
			}
		}

		logger.info("Authentication successful for user \"" + context.getUserName() + '"');
		isLoggedIn = true;
		return true;
	}

	protected void checkLoggedIn() {
		if (! isLoggedIn) {
			throw new IllegalStateException();
		}
	}

	@Override
	public MessagesMetaData<MessageMetaData> getMessagesMetaData() throws IOException {
		if (allMessagesCache != null) {
			return allMessagesCache;
		}
		allMessagesCache = getMessagesMetaData(null, null, Integer.MAX_VALUE);
		return allMessagesCache;
	}
	@Override
	public MessagesMetaData<MessageMetaData> getMessagesMetaData(int count) throws IOException {
		return getMessagesMetaData(null, null, count);
	}
	@Override
	public MessagesMetaData<MessageMetaData> getMessagesMetaData(Date oldestMessageToFetch) throws IOException {
		return getMessagesMetaData(oldestMessageToFetch, null, Integer.MAX_VALUE);
	}
	@Override
	public MessagesMetaData<MessageMetaData> getMessagesMetaData(Date oldestMessageToFetch, Date newestMessageToFetch) throws IOException {
		return getMessagesMetaData(oldestMessageToFetch, newestMessageToFetch, Integer.MAX_VALUE);
	}
	protected MessagesMetaData<MessageMetaData> getMessagesMetaData(Date oldestMessageToFetch, Date newestMessageToFetch, int count) throws IOException {
		checkLoggedIn();
		if (oldestMessageToFetch == null) {
			oldestMessageToFetch = new Date(0);
		}
		if (newestMessageToFetch == null) {
			newestMessageToFetch = new Date(Long.MAX_VALUE);
		}
		// iNotes limits the number of results to 1000. Need to paginate.
		int start = 1, currentCount = 0;
		MessagesMetaData<MessageMetaData> messages = null, partialMessages;
		boolean stopLoading = false;
		do {
			partialMessages = getMessagesMetaDataNoSort(start, Math.min(count - currentCount, META_DATA_LOAD_BATCH_SIZE));
			if (messages == null) {
				messages = partialMessages;
				// filter on date
				Iterator<MessageMetaData> iterator = messages.entries.iterator();
				while (iterator.hasNext()) {
					BaseINotesMessage message = iterator.next();
					if (message.getDate().before(oldestMessageToFetch)) {
						iterator.remove();
					} else if (message.getDate().after(newestMessageToFetch)) {
						iterator.remove();
					}
				}
			} else {
				for (MessageMetaData message : partialMessages.entries) {
					if (message.getDate().before(oldestMessageToFetch)) {
						stopLoading = true;
						break;
					} else if (message.getDate().after(newestMessageToFetch)) {
						stopLoading = false;
						continue;
					} else {
						messages.entries.add(message);
					}
				}
			}
			start += META_DATA_LOAD_BATCH_SIZE;
			currentCount = messages.entries.size();
		} while (! stopLoading && partialMessages.entries.size() >= Math.min(count - currentCount, META_DATA_LOAD_BATCH_SIZE) && currentCount < count);
		Collections.reverse(messages.entries);
		logger.trace("Loaded {} messages metadata", Integer.valueOf(messages.entries.size()));
		return messages;
	}

	/**
	 * @return set of messages meta-data, in iNotes order (most recent first)
	 */
	protected MessagesMetaData<MessageMetaData> getMessagesMetaDataNoSort(int start, int count) throws IOException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "s_ReadViewEntries");
//		params.put("PresetFields", "DBQuotaInfo;1,FolderName;"+context.getNotesFolderName()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1");
		params.put("TZType", "UTC");
		params.put("Start", Integer.toString(start));
		params.put("Count", Integer.toString(count));
		params.put("resortdescending", "5");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getProxyBaseURL()+"&PresetFields=DBQuotaInfo;1,FolderName;"+context.getCurrentFolderId()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
//		traceBody(httpResponse);// DEBUG
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching messages meta-data for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		MessagesMetaData<MessageMetaData> messages;
		try {
			messages = new MessagesXMLConverter().convertXML(httpResponse.getBody(), context.getCharset(httpResponse));
		} catch (XMLStreamException e) {
			throw new IOException(e);
		} finally {
			httpResponse.close();
		}
		return messages;
	}

	/**
	 * Don't forget to call {@link LineIterator#close()} when done with the response!
	 *
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws MailParseException if the content of the email is invalid (i.e. iNotes http session has expired)
	 */
	public IteratorChain<String> getMessageMIMEHeaders(MessageMetaData message) throws IOException, MailParseException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "l_MailMessageHeader");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getFolderBaseURL()+message.getId()+"/?OpenDocument"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching message MIME headers for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		LineIterator responseLines = new HttpCleaningLineIterator(httpResponse);
		//httpResponse.close();// done in HttpLineIterator#close()
		if (message.unread) {
			// exporting (read MIME) marks mail as read. Need to get the read/unread information and set it back!
			mailsToMarkUnread.add(message.getId());
		}
		return new IteratorChain<String>(getINotesData(message).iterator(), responseLines);
	}

	/**
	 * Don't forget to call {@link LineIterator#close()} when done with the response!
	 *
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws MailParseException if the content of the email is invalid (i.e. iNotes http session has expired)
	 */
	public IteratorChain<String> getMessageMIME(MessageMetaData message) throws IOException, MailParseException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "l_MailMessageHeader");
//		params.put("PresetFields", "FullMessage;1");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getFolderBaseURL()+message.getId()+"/?OpenDocument&PresetFields=FullMessage;1"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching message MIME for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		LineIterator responseLines = new HttpCleaningLineIterator(httpResponse);
		//httpResponse.close();// done in HttpLineIterator#close()
		if (message.unread) {
			// exporting (read MIME) marks mail as read. Need to get the read/unread information and set it back!
			mailsToMarkUnread.add(message.getId());
		}
		return new IteratorChain<String>(getINotesData(message).iterator(), responseLines);
	}

	protected List<String> getINotesData(MessageMetaData message) throws IOException {
		List<String> iNotes = new ArrayList<String>(5);
		iNotes.add("X-iNotes-unid: " + message.unid);
		iNotes.add("X-iNotes-noteid: " + message.noteid);
		iNotes.add("X-iNotes-unread: " + message.unread);
		iNotes.add("X-iNotes-date: " + fr.cedrik.util.DateUtils.RFC2822_DATE_TIME_FORMAT.format(message.date));
		iNotes.add("X-iNotes-size: " + message.size);
		return Collections.unmodifiableList(iNotes);
	}

	protected List<String> getINotesData(MeetingNoticeMetaData message) throws IOException {
		List<String> iNotes = new ArrayList<String>(5);
		iNotes.add("X-iNotes-unid: " + message.unid);
		iNotes.add("X-iNotes-noteid: " + message.noteid);
		iNotes.add("X-iNotes-date: " + fr.cedrik.util.DateUtils.RFC2822_DATE_TIME_FORMAT.format(message.date));
		return Collections.unmodifiableList(iNotes);
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void deleteMessage(MessageMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MessageMetaData message : messages) {
			mailsToDelete.add(message.getId());
			mailsToMarkRead.remove(message.getId());
			mailsToMarkUnread.remove(message.getId());
		}
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void deleteMessage(MeetingNoticeMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MeetingNoticeMetaData message : messages) {
			noticesToDelete.add(message.getId());
		}
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	@Override
	public void deleteMessage(Collection<? extends Message> messages) throws IOException {
		deleteMessage(messages.toArray(new Message[messages.size()]));
	}
	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	@Override
	public void deleteMessage(Message... messages) throws IOException {
		checkLoggedIn();
		for (Message message : messages) {
			if (message instanceof MessageMetaData) {
				deleteMessage((MessageMetaData) message);
			} else if (message instanceof MeetingNoticeMetaData) {
				deleteMessage((MeetingNoticeMetaData) message);
			} else {
				throw new IllegalArgumentException(message.getClass().toString());
			}
		}
	}

	@Override
	public void undeleteAllMessages() {
		checkLoggedIn();
		mailsToDelete.clear();
		mailsToMarkRead.clear();
		mailsToMarkRead.addAll(mailsToMarkReadAll);
		mailsToMarkUnread.clear();
		mailsToMarkUnread.addAll(mailsToMarkUnreadAll);
		noticesToDelete.clear();
	}

	protected void doDeleteMessages() throws IOException {
		if (mailsToDelete.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_AllDocs", "");
		params.put("h_FolderStorage", "");
		params.put("s_ViewName", context.getCurrentFolderId());
		params.put("h_SetCommand", "h_DeletePages");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", StringUtils.join(mailsToDelete, ';'));
		params.put("h_SetDeleteListCS", "");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getMailEditBaseURL()), HttpMethod.POST, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (logger.isTraceEnabled()) {
			traceBody(httpResponse);
		}
		try {
			if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
				logger.error("Unknown server response while deleting messages for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
				return;
			}
		} finally {
			httpResponse.close();
		}
		logger.info("Deleted (moved to Trash) {} messsage(s): {}", mailsToDelete.size(), StringUtils.join(mailsToDelete, ';'));
		mailsToMarkReadAll.removeAll(mailsToDelete);
		mailsToMarkUnreadAll.removeAll(mailsToDelete);
		mailsToDelete.clear();
	}

	protected void doDeleteNotices() throws IOException {
		if (noticesToDelete.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_AllDocs", "");
		params.put("h_FolderStorage", "");
		params.put("s_ViewName", Folder.MEETING_NOTICES);
		params.put("h_SetCommand", "h_ShimmerCSRemoveFromFolder");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", "");
		params.put("h_SetDeleteListCS", StringUtils.join(noticesToDelete, ';'));
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getMailEditBaseURL()), HttpMethod.POST, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (logger.isTraceEnabled()) {
			traceBody(httpResponse);
		}
		try {
			if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
				logger.error("Unknown server response while removing notices from view for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
				return;
			}
		} finally {
			httpResponse.close();
		}
		logger.info("Deleted (removed from view) {} messsage(s): {}", noticesToDelete.size(), StringUtils.join(noticesToDelete, ';'));
		noticesToDelete.clear();
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void markMessagesRead(MessageMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MessageMetaData message : messages) {
			String id = message.getId();
			mailsToMarkUnreadAll.remove(id);
			mailsToMarkReadAll.add(id);
			if (! mailsToDelete.contains(id)) {
				mailsToMarkUnread.remove(id);
				mailsToMarkRead.add(id);
			}
		}
	}

	protected void doMarkMessagesRead() throws IOException {
		if (mailsToMarkRead.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
		params.put("s_ViewName", context.getCurrentFolderId());
		params.put("h_AllDocs", "");
		params.put("h_SetCommand", "h_ShimmerMarkRead");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", StringUtils.join(mailsToMarkRead, ';'));
		params.put("h_SetDeleteListCS", "");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getMailEditBaseURL()), HttpMethod.POST, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (logger.isTraceEnabled()) {
			traceBody(httpResponse);
		}
		try {
			if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
				logger.error("Unknown server response while marking messages read for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
				return;
			}
		} finally {
			httpResponse.close();
		}
		logger.info("Marked {} messsage(s) as read in folder {}: {}", mailsToMarkRead.size(), context.getCurrentFolderId(), StringUtils.join(mailsToMarkRead, ';'));
		mailsToMarkReadAll.removeAll(mailsToMarkRead);
		mailsToMarkRead.clear();
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void markMessagesUnread(MessageMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MessageMetaData message : messages) {
			String id = message.getId();
			mailsToMarkReadAll.remove(id);
			mailsToMarkUnreadAll.add(id);
			if (! mailsToDelete.contains(id)) {
				mailsToMarkRead.remove(id);
				mailsToMarkUnread.add(id);
			}
		}
	}

	protected void doMarkMessagesUnread() throws IOException {
		if (mailsToMarkUnread.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
//		params.put("PresetFields", "s_NoMarkRead;1");
		params.put("s_ViewName", context.getCurrentFolderId());
		params.put("h_AllDocs", "");
		params.put("h_SetCommand", "h_ShimmerMarkUnread");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", StringUtils.join(mailsToMarkUnread, ';'));
		params.put("h_SetDeleteListCS", "");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getMailEditBaseURL()+"&PresetFields=s_NoMarkRead;1"), HttpMethod.POST, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (logger.isTraceEnabled()) {
			traceBody(httpResponse);
		}
		try {
			if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
				logger.error("Unknown server response while marking messages unread for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
				return;
			}
		} finally {
			httpResponse.close();
		}
		logger.info("Marked {} messsage(s) as unread in folder {}: {}", mailsToMarkUnread.size(), context.getCurrentFolderId(), StringUtils.join(mailsToMarkUnread, ';'));
		mailsToMarkUnreadAll.removeAll(mailsToMarkUnread);
		mailsToMarkUnread.clear();
	}


	public MessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData() throws IOException {
		if (allNoticesCache != null) {
			return allNoticesCache;
		}
		allNoticesCache = getMeetingNoticesMetaData(null, null, Integer.MAX_VALUE);
		return allNoticesCache;
	}
	public MessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(int count) throws IOException {
		return getMeetingNoticesMetaData(null, null, count);
	}
	public MessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(Date oldestMessageToFetch) throws IOException {
		return getMeetingNoticesMetaData(oldestMessageToFetch, null, Integer.MAX_VALUE);
	}
	public MessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(Date oldestMessageToFetch, Date newestMessageToFetch) throws IOException {
		return getMeetingNoticesMetaData(oldestMessageToFetch, newestMessageToFetch, Integer.MAX_VALUE);
	}
	public MessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(Date oldestMessageToFetch, Date newestMessageToFetch, int count) throws IOException {
		checkLoggedIn();
		cleanup();
		if (oldestMessageToFetch == null) {
			oldestMessageToFetch = new Date(0);
		}
		if (newestMessageToFetch == null) {
			newestMessageToFetch = new Date(Long.MAX_VALUE);
		}
		String notesFolderIdBackup = context.getCurrentFolderId();
		context.setCurrentFolderId(Folder.MEETING_NOTICES);
		// iNotes limits the number of results to 1000. Need to paginate.
		int start = 1, currentCount = 0;
		MessagesMetaData<MeetingNoticeMetaData> notices = null, partialNotices;
		boolean stopLoading = false;
		do {
			partialNotices = getMeetingNoticesMetaDataNoSort(start, Math.min(count - currentCount, META_DATA_LOAD_BATCH_SIZE));
			if (notices == null) {
				notices = partialNotices;
				// filter on date
				Iterator<MeetingNoticeMetaData> iterator = notices.entries.iterator();
				while (iterator.hasNext()) {
					BaseINotesMessage notice = iterator.next();
					if (notice.getDate().before(oldestMessageToFetch)) {
						iterator.remove();
					} else if (notice.getDate().after(newestMessageToFetch)) {
						iterator.remove();
					}
				}
			} else {
				for (MeetingNoticeMetaData notice : partialNotices.entries) {
					if (notice.getDate().before(oldestMessageToFetch)) {
						stopLoading = true;
						break;
					} else if (notice.getDate().after(newestMessageToFetch)) {
						stopLoading = false;
						continue;
					} else {
						notices.entries.add(notice);
					}
				}
			}
			start += META_DATA_LOAD_BATCH_SIZE;
			currentCount = notices.entries.size();
		} while (! stopLoading && partialNotices.entries.size() >= Math.min(count - currentCount, META_DATA_LOAD_BATCH_SIZE) && currentCount < count);
		context.setCurrentFolderId(notesFolderIdBackup);
		Collections.reverse(notices.entries);
		logger.trace("Loaded {} meeting notices metadata", Integer.valueOf(notices.entries.size()));
		return notices;
	}

	/**
	 * @return set of meeting notices meta-data, in iNotes order
	 */
	protected MessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaDataNoSort(int start, int count) throws IOException {
		checkLoggedIn();
		String notesFolderIdBackup = context.getCurrentFolderId();
		context.setCurrentFolderId(Folder.MEETING_NOTICES);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "s_ReadViewEntries");
//		params.put("PresetFields", "DBQuotaInfo;1,FolderName;"+context.getNotesFolderName()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1");
		params.put("TZType", "UTC");
		params.put("Start", Integer.toString(start));
		params.put("Count", Integer.toString(count));
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getProxyBaseURL()+"&PresetFields=DBQuotaInfo;1,FolderName;"+Folder.MEETING_NOTICES+",s_UsingHttps;1,hc;AltChair,noPI;1"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
//		traceBody(httpResponse);// DEBUG
		context.setCurrentFolderId(notesFolderIdBackup);
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching metting notices meta-data for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		MessagesMetaData<MeetingNoticeMetaData> notices;
		try {
			notices = new MeetingNoticesXMLConverter().convertXML(httpResponse.getBody(), context.getCharset(httpResponse));
		} catch (XMLStreamException e) {
			throw new IOException(e);
		} finally {
			httpResponse.close();
		}
		return notices;
	}

//	public Calendar getMeetingNoticeICS(MeetingNoticeMetaData meetingNotice) throws IOException {
//		checkLoggedIn();
//		String notesFolderIdBackup = context.getNotesFolderId();
//		context.setNotesFolderId(Folder.MEETING_NOTICES);
//		Map<String, Object> params = new HashMap<String, Object>();
//		params.put("charset", CharEncoding.UTF_8);
//		params.put("Form", "l_JSVars");
//		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getFolderBaseURL()+meetingNotice.getId()+"/?OpenDocument"+"&PresetFields=s_HandleAttachmentNames;1,s_HandleMime;1,s_OpenUI;1,s_HideRemoteImage;1"), HttpMethod.GET, params);
//		ClientHttpResponse httpResponse = httpRequest.execute();
//		trace(httpRequest, httpResponse);
//		context.setNotesFolderId(notesFolderIdBackup);
//		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
//			logger.error("Unknown server response while fetching Meeting Notice for user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
//			httpResponse.close();
//			return null;
//		}
//		Calendar ics;
//		try {
//			ics = new MeetingNoticeJSONConverter().convertJSON(httpResponse.getBody(), context.getCharset(httpResponse));
//		} finally {
//			httpResponse.close();
//		}
//		return ics;
//	}
//
//	/**
//	 * Don't forget to call {@link LineIterator#close()} when done with the response!
//	 *
//	 * @param message
//	 * @return
//	 * @throws IOException
//	 */
//	public IteratorChain<String> getMessageMIME(MeetingNoticeMetaData message) throws IOException {
//		Calendar ics = getMeetingNoticeICS(message);
//		try {
//			Part email = ICSUtils.toEMail(ics);
//			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, (int)(email.getSize()*1.1)));
//			email.writeTo(out);
//			BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
//			out = null;
//			return new IteratorChain<String>(getINotesData(message).iterator(), new LineIterator(in));
//		} catch (MessagingException e) {
//			throw new IOException(e);
//		}
//	}
//
//	/**
//	 * Don't forget to call {@link LineIterator#close()} when done with the response!
//	 *
//	 * @param message
//	 * @return
//	 * @throws IOException
//	 */
//	public IteratorChain<String> getMessageMIMEHeaders(MeetingNoticeMetaData message) throws IOException {
//		Calendar ics = getMeetingNoticeICS(message);
//		try {
//			Part email = ICSUtils.toEMail(ics);
//			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, (int)(email.getSize()*1.1)));
//			email.writeTo(out);
//			BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
//			out = null;
//			List<String> headers = new ArrayList<String>();
//			String currentLine;
//			while (StringUtils.isNotBlank(currentLine = in.readLine())) {
//				headers.add(currentLine);
//			}
//			return new IteratorChain<String>(getINotesData(message).iterator(), headers.iterator());
//		} catch (MessagingException e) {
//			throw new IOException(e);
//		}
//	}


	/**
	 * Don't forget to call {@link LineIterator#close()} when done with the response!
	 *
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws MailParseException if the content of the email is invalid (i.e. iNotes http session has expired)
	 */
	@Override
	public IteratorChain<String> getMessageMIME(Message message) throws IOException, MailParseException {
		if (message instanceof MessageMetaData) {
			return getMessageMIME((MessageMetaData)message);
//		} else if (message instanceof MeetingNoticeMetaData) {
//			return getMessageMIME((MeetingNoticeMetaData)message);
		} else {
			throw new IllegalArgumentException(message.getClass().toString());
		}
	}

	/**
	 * Don't forget to call {@link LineIterator#close()} when done with the response!
	 *
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws MailParseException if the content of the email is invalid (i.e. iNotes http session has expired)
	 */
	@Override
	public IteratorChain<String> getMessageMIMEHeaders(Message message) throws IOException, MailParseException {
		if (message instanceof MessageMetaData) {
			return getMessageMIMEHeaders((MessageMetaData)message);
//		} else if (message instanceof MeetingNoticeMetaData) {
//			return getMessageMIMEHeaders((MeetingNoticeMetaData)message);
		} else {
			throw new IllegalArgumentException(message.getClass().toString());
		}
	}


	public MessagesMetaData<? extends Message> getMessagesAndMeetingNoticesMetaData() throws IOException {
		return getMessagesMetaData();
		//FIXME uncomment when we find a way to export meeting invites!
//		if (Folder.INBOX.equals(context.getNotesFolderId()) || Folder.ALL.equals(context.getNotesFolderId())) {
//			MessagesMetaData<MessageMetaData> messagesMetaData = getMessagesMetaData();
//			MessagesMetaData<MeetingNoticeMetaData> noticesMetaData = getMeetingNoticesMetaData();
//			MessagesMetaData<Message> result = messagesMetaData.clone();
//			result.entries.clear();
//			result.entries.addAll(messagesMetaData.entries);
//			result.entries.addAll(noticesMetaData.entries);
//			Collections.sort(result.entries, new Comparator<Message>() {
//				@Override
//				public int compare(Message o1, Message o2) {
//					return o1.getDate().compareTo(o2.getDate());
//				}
//			});
//			return result;
//		} else {
//			return getMessagesMetaData();
//		}
	}

	public MessagesMetaData<? extends BaseINotesMessage> getMessagesAndMeetingNoticesMetaData(Date oldestMessageToFetch) throws IOException {
		return getMessagesAndMeetingNoticesMetaData(oldestMessageToFetch, null);
	}

	public MessagesMetaData<? extends BaseINotesMessage> getMessagesAndMeetingNoticesMetaData(Date oldestMessageToFetch, Date newestMessageToFetch) throws IOException {
		return getMessagesMetaData(oldestMessageToFetch, newestMessageToFetch);
		//FIXME uncomment when we find a way to export meeting invites!
//		if (Folder.INBOX.equals(context.getNotesFolderId()) || Folder.ALL.equals(context.getNotesFolderId())) {
//			MessagesMetaData<MessageMetaData> messagesMetaData = getMessagesMetaData(oldestMessageToFetch, newestMessageToFetch);
//			MessagesMetaData<MeetingNoticeMetaData> noticesMetaData = getMeetingNoticesMetaData(oldestMessageToFetch, newestMessageToFetch);
//			MessagesMetaData<Message> result = messagesMetaData.clone();
//			result.entries.clear();
//			result.entries.addAll(messagesMetaData.entries);
//			result.entries.addAll(noticesMetaData.entries);
//			Collections.sort(result.entries, new Comparator<Message>() {
//				@Override
//				public int compare(Message o1, Message o2) {
//					return o1.getDate().compareTo(o2.getDate());
//				}
//			});
//			return result;
//		} else {
//			return getMessagesMetaData(oldestMessageToFetch);
//		}
	}



	protected void cleanup() throws IOException {
		// do mark messages unread
		doMarkMessagesUnread();
		// do mark messages read
		doMarkMessagesRead();
		// do delete messages
		doDeleteMessages();
		// do delete notices
		doDeleteNotices();
	}

	@Override
	public boolean logout() throws IOException {
		if (! isLoggedIn) {
			return true;
		}
		cleanup();
		// and now: logout!
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "s_Logout");
//		params.put("PresetFields", "s_CacheScrubType;0");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getProxyBaseURL()+"&PresetFields=s_CacheScrubType;0"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		context.rememberCookies(httpRequest, httpResponse);
		if (logger.isTraceEnabled()) {
			traceBody(httpResponse);
		}
		try {
			if (httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
				logger.info("Logout successful for user \"" + context.getUserName() + '"');
			} else {
				logger.warn("ERROR while logging out user \""+context.getUserName()+"\": " + httpResponse.getRawStatusCode() + ' ' + httpResponse.getStatusText());
				return false;
			}
		} finally {
			httpResponse.close();
		}
		context.getCookieStore().removeAll();
		isLoggedIn = false;
		allMessagesCache = null;
		allNoticesCache = null;
		mailsToDelete.clear();
		mailsToMarkRead.clear();
		mailsToMarkReadAll.clear();
		mailsToMarkUnread.clear();
		mailsToMarkUnreadAll.clear();
		noticesToDelete.clear();
		return true;
	}


	private class HttpCleaningLineIterator extends LineIterator implements Iterator<String>, Closeable {
		private final Logger logger = LoggerFactory.getLogger(HttpCleaningLineIterator.class);
		private final ClientHttpResponse httpResponse;
		private boolean inHeaders = true;
		private boolean firstHeaderLine = true;

		public HttpCleaningLineIterator(final ClientHttpResponse httpResponse) throws IOException, MailParseException {
			super(new InputStreamReader(httpResponse.getBody(), context.getCharset(httpResponse)));
			this.httpResponse = httpResponse;
			if (context.isFixLotusNotesDateMIMEHeader()) {
				inHeaders = true;
			} else {
				inHeaders = false;
			}
			hasNext();// will call isValidLine() to validate first line
		}

		@Override
		protected boolean isValidLine(String line) throws MailParseException {
			if (firstHeaderLine) {
				firstHeaderLine = false;
				// check that this is a correct RFC 5322 Header Field line
				if (! MIME_HEADER.matcher(line).matches()) {
					close();
//					throw new IllegalStateException("Bad MIME header: " + line);
//					throw new java.util.IllegalFormatException("Bad MIME header: " + line);
//					throw new java.text.ParseException("Bad MIME header: " + line, 0);
//					throw new java.util.regex.PatternSyntaxException("Bad MIME header", line, 0);
//					throw new javax.mail.internet.ParseException("Bad MIME header: " + line);
//					throw new javax.activation.MimeTypeParseException("Bad MIME header: " + line);
					throw new org.springframework.mail.MailParseException("Bad MIME header: " + line);
				}
			}
			return super.isValidLine(line);
		}

		@Override
		public String nextLine() {
			String line = super.nextLine();
			CharSequence data = line;
			// delete html tags
			if (line.endsWith("<br>")) {
				data = new StringBuilder(line).delete(line.length()-"<br>".length(), line.length());
			}
			// convert &quot; -> ", &amp; -> &, &lt; -> <, &gt; -> >
			line = new LookupTranslator(EntityArrays.BASIC_UNESCAPE()).translate(data);
			if (StringUtils.isEmpty(line)) {
				inHeaders = false;
			}
			// fix Lotus Notes broken date pattern: 29-Oct-2012 19:23:20 CET	10-Oct-2012 11:25:11 CEDT
			if (inHeaders) {
				line = fr.cedrik.util.DateUtils.fixLotusMIMEDateHeader(line);
			}
			return line;
		}

		@Override
		public void close() {
			super.close();
			httpResponse.close();
		}
	}

	static final Pattern MIME_HEADER = Pattern.compile("^[^:]+:.*$");//$NON-NLS-1$

}
