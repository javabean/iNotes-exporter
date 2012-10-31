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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class Session {
	private static final int META_DATA_LOAD_BATCH_SIZE = 500;

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected final HttpContext context;
	protected final Set<String> mailsToDelete = new HashSet<String>();
	protected final Set<String> mailsToMarkUnread = new HashSet<String>();
	protected final Set<String> mailsToMarkRead = new HashSet<String>();
	protected final Set<String> mailsToMarkUnreadAll = new HashSet<String>();
	protected final Set<String> mailsToMarkReadAll = new HashSet<String>();
	protected final Set<String> noticesToDelete = new HashSet<String>();
	protected INotesMessagesMetaData<MessageMetaData> allMessagesCache = null;
	protected INotesMessagesMetaData<MeetingNoticeMetaData> allNoticesCache = null;
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
			logger.debug(httpRequest.getMethod().toString() + ' ' + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText() + ' ' + httpRequest.getURI());
		}
	}
	private void traceBody(ClientHttpResponse httpResponse) throws IOException {
		if (logger.isTraceEnabled()) {
			String responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
			logger.trace(responseBody);
		}
	}

	public void setServerAddress(URL url) {
		if (isLoggedIn) {
			throw new IllegalStateException();
		}
		context.iNotes.setServerAddress(url);
	}

	public FoldersList getFolders() {
		if (! isLoggedIn) {
			throw new IllegalStateException();
		}
		return folders;
	}

	public void setCurrentFolder(Folder folder) throws IOException {
		if (isLoggedIn) {
			cleanup();
		}
		context.setNotesFolderId(folder.id);
		allMessagesCache = null;
		allNoticesCache = null;
	}

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
					logger.info("Authentication successful for user \"" + context.getUserName() + '"');
					logger.debug("Redirect: {}", httpResponse.getHeaders().getLocation());
				} else if (httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
					// body will contain "Invalid username or password was specified."
					logger.warn("ERROR while authenticating user \""+context.getUserName()+"\". Please check your parameters in " + INotesProperties.FILE);
					return false;
				} else {
					logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
					logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
			context.getHttpHeaders().set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:14.0) Gecko/20100101 Firefox/14.0.1");
//			context.getHttpHeaders().set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/534.57.7 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.7");
			httpRequest = context.createRequest(new URL(redirectURL), HttpMethod.GET, params);
			httpResponse = httpRequest.execute();
			trace(httpRequest, httpResponse);
			context.rememberCookies(httpRequest, httpResponse);
			responseBody = IOUtils.toString(httpResponse.getBody(), context.getCharset(httpResponse));
			// Apparently we don't need to parse the embeded JS to set the "Shimmer" cookie.
			try {
				if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
					logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
						logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
						logger.error("Unknown server response while authenticating user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
				List<String> excludedFoldersIds = context.getNotesExcludedFoldersIds();
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

		isLoggedIn = true;
		return true;
	}

	protected void checkLoggedIn() {
		if (! isLoggedIn) {
			throw new IllegalStateException();
		}
	}

	public INotesMessagesMetaData<MessageMetaData> getMessagesMetaData() throws IOException {
		if (allMessagesCache != null) {
			return allMessagesCache;
		}
		allMessagesCache = getMessagesMetaData(null, Integer.MAX_VALUE);
		return allMessagesCache;
	}
	public INotesMessagesMetaData<MessageMetaData> getMessagesMetaData(int count) throws IOException {
		return getMessagesMetaData(null, count);
	}
	public INotesMessagesMetaData<MessageMetaData> getMessagesMetaData(Date oldestMessageToFetch) throws IOException {
		return getMessagesMetaData(oldestMessageToFetch, Integer.MAX_VALUE);
	}
	protected INotesMessagesMetaData<MessageMetaData> getMessagesMetaData(Date oldestMessageToFetch, int count) throws IOException {
		checkLoggedIn();
		if (oldestMessageToFetch == null) {
			oldestMessageToFetch = new Date(0);
		}
		// iNotes limits the number of results to 1000. Need to paginate.
		int start = 1, currentCount = 0;
		INotesMessagesMetaData<MessageMetaData> messages = null, partialMessages;
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
					}
				}
			} else {
				for (MessageMetaData message : partialMessages.entries) {
					if (message.getDate().before(oldestMessageToFetch)) {
						stopLoading = true;
						break;
					}
					messages.entries.add(message);
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
	protected INotesMessagesMetaData<MessageMetaData> getMessagesMetaDataNoSort(int start, int count) throws IOException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "s_ReadViewEntries");
//		params.put("PresetFields", "DBQuotaInfo;1,FolderName;"+context.getNotesFolderName()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1");
		params.put("TZType", "UTC");
		params.put("Start", Integer.toString(start));
		params.put("Count", Integer.toString(count));
		params.put("resortdescending", "5");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getProxyBaseURL()+"&PresetFields=DBQuotaInfo;1,FolderName;"+context.getNotesFolderId()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
//		traceBody(httpResponse);// DEBUG
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching messages meta-data for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		INotesMessagesMetaData<MessageMetaData> messages;
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
	 */
	public IteratorChain<String> getMessageMIMEHeaders(MessageMetaData message) throws IOException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "l_MailMessageHeader");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getFolderBaseURL()+message.unid+"/?OpenDocument"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching message MIME headers for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		LineIterator responseLines = new HttpCleaningLineIterator(httpResponse);
		//httpResponse.close();// done in HttpLineIterator#close()
		if (message.unread) {
			// exporting (read MIME) marks mail as read. Need to get the read/unread information and set it back!
			mailsToMarkUnread.add(message.unid);
		}
		return new IteratorChain<String>(getINotesData(message).iterator(), responseLines);
	}

	/**
	 * Don't forget to call {@link LineIterator#close()} when done with the response!
	 *
	 * @param message
	 * @return
	 * @throws IOException
	 */
	public IteratorChain<String> getMessageMIME(MessageMetaData message) throws IOException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "l_MailMessageHeader");
//		params.put("PresetFields", "FullMessage;1");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getFolderBaseURL()+message.unid+"/?OpenDocument&PresetFields=FullMessage;1"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching message MIME for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		LineIterator responseLines = new HttpCleaningLineIterator(httpResponse);
		//httpResponse.close();// done in HttpLineIterator#close()
		if (message.unread) {
			// exporting (read MIME) marks mail as read. Need to get the read/unread information and set it back!
			mailsToMarkUnread.add(message.unid);
		}
		return new IteratorChain<String>(getINotesData(message).iterator(), responseLines);
	}

	protected List<String> getINotesData(MessageMetaData message) throws IOException {
		List<String> iNotes = new ArrayList<String>(5);
		iNotes.add("X-iNotes-unid: " + message.unid);
		iNotes.add("X-iNotes-noteid: " + message.noteid);
		iNotes.add("X-iNotes-unread: " + message.unread);
		iNotes.add("X-iNotes-date: " + fr.cedrik.inotes.util.DateUtils.RFC2822_DATE_TIME_FORMAT.format(message.date));
		iNotes.add("X-iNotes-size: " + message.size);
		return Collections.unmodifiableList(iNotes);
	}

	protected List<String> getINotesData(MeetingNoticeMetaData message) throws IOException {
		List<String> iNotes = new ArrayList<String>(5);
		iNotes.add("X-iNotes-unid: " + message.unid);
		iNotes.add("X-iNotes-noteid: " + message.noteid);
		iNotes.add("X-iNotes-date: " + fr.cedrik.inotes.util.DateUtils.RFC2822_DATE_TIME_FORMAT.format(message.date));
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
			mailsToDelete.add(message.unid);
			mailsToMarkRead.remove(message.unid);
			mailsToMarkUnread.remove(message.unid);
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
			noticesToDelete.add(message.unid);
		}
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void deleteMessage(BaseINotesMessage... messages) throws IOException {
		checkLoggedIn();
		for (BaseINotesMessage message : messages) {
			if (message instanceof MessageMetaData) {
				deleteMessage((MessageMetaData) message);
			} else if (message instanceof MeetingNoticeMetaData) {
				deleteMessage((MeetingNoticeMetaData) message);
			} else {
				throw new IllegalArgumentException(message.getClass().toString());
			}
		}
	}

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
		params.put("s_ViewName", context.getNotesFolderId());
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
				logger.error("Unknown server response while deleting messages for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
				logger.error("Unknown server response while removing notices from view for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
			mailsToMarkUnreadAll.remove(message.unid);
			mailsToMarkReadAll.add(message.unid);
			if (! mailsToDelete.contains(message.unid)) {
				mailsToMarkUnread.remove(message.unid);
				mailsToMarkRead.add(message.unid);
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
		params.put("s_ViewName", context.getNotesFolderId());
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
				logger.error("Unknown server response while marking messages read for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
				return;
			}
		} finally {
			httpResponse.close();
		}
		logger.info("Marked {} messsage(s) as read: {}", mailsToMarkRead.size(), StringUtils.join(mailsToMarkRead, ';'));
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
			mailsToMarkReadAll.remove(message.unid);
			mailsToMarkUnreadAll.add(message.unid);
			if (! mailsToDelete.contains(message.unid)) {
				mailsToMarkRead.remove(message.unid);
				mailsToMarkUnread.add(message.unid);
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
		params.put("s_ViewName", context.getNotesFolderId());
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
				logger.error("Unknown server response while marking messages unread for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
				return;
			}
		} finally {
			httpResponse.close();
		}
		logger.info("Marked {} messsage(s) as unread: {}", mailsToMarkUnread.size(), StringUtils.join(mailsToMarkUnread, ';'));
		mailsToMarkUnreadAll.removeAll(mailsToMarkUnread);
		mailsToMarkUnread.clear();
	}


	public INotesMessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData() throws IOException {
		if (allNoticesCache != null) {
			return allNoticesCache;
		}
		allNoticesCache = getMeetingNoticesMetaData(null, Integer.MAX_VALUE);
		return allNoticesCache;
	}
	public INotesMessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(int count) throws IOException {
		return getMeetingNoticesMetaData(null, count);
	}
	public INotesMessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(Date oldestMessageToFetch) throws IOException {
		return getMeetingNoticesMetaData(oldestMessageToFetch, Integer.MAX_VALUE);
	}
	public INotesMessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaData(Date oldestMessageToFetch, int count) throws IOException {
		checkLoggedIn();
		cleanup();
		if (oldestMessageToFetch == null) {
			oldestMessageToFetch = new Date(0);
		}
		String notesFolderIdBackup = context.getNotesFolderId();
		context.setNotesFolderId(Folder.MEETING_NOTICES);
		// iNotes limits the number of results to 1000. Need to paginate.
		int start = 1, currentCount = 0;
		INotesMessagesMetaData<MeetingNoticeMetaData> notices = null, partialNotices;
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
					}
				}
			} else {
				for (MeetingNoticeMetaData notice : partialNotices.entries) {
					if (notice.getDate().before(oldestMessageToFetch)) {
						stopLoading = true;
						break;
					}
					notices.entries.add(notice);
				}
			}
			start += META_DATA_LOAD_BATCH_SIZE;
			currentCount = notices.entries.size();
		} while (! stopLoading && partialNotices.entries.size() >= Math.min(count - currentCount, META_DATA_LOAD_BATCH_SIZE) && currentCount < count);
		context.setNotesFolderId(notesFolderIdBackup);
		Collections.reverse(notices.entries);
		logger.trace("Loaded {} meeting notices metadata", Integer.valueOf(notices.entries.size()));
		return notices;
	}

	/**
	 * @return set of meeting notices meta-data, in iNotes order
	 */
	protected INotesMessagesMetaData<MeetingNoticeMetaData> getMeetingNoticesMetaDataNoSort(int start, int count) throws IOException {
		checkLoggedIn();
		String notesFolderIdBackup = context.getNotesFolderId();
		context.setNotesFolderId(Folder.MEETING_NOTICES);
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
		context.setNotesFolderId(notesFolderIdBackup);
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching metting notices meta-data for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		INotesMessagesMetaData<MeetingNoticeMetaData> notices;
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
//		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getFolderBaseURL()+meetingNotice.unid+"/?OpenDocument"+"&PresetFields=s_HandleAttachmentNames;1,s_HandleMime;1,s_OpenUI;1,s_HideRemoteImage;1"), HttpMethod.GET, params);
//		ClientHttpResponse httpResponse = httpRequest.execute();
//		trace(httpRequest, httpResponse);
//		context.setNotesFolderId(notesFolderIdBackup);
//		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
//			logger.error("Unknown server response while fetching Meeting Notice for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
	 */
	public IteratorChain<String> getMessageMIME(BaseINotesMessage message) throws IOException {
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
	 */
	public IteratorChain<String> getMessageMIMEHeaders(BaseINotesMessage message) throws IOException {
		if (message instanceof MessageMetaData) {
			return getMessageMIMEHeaders((MessageMetaData)message);
//		} else if (message instanceof MeetingNoticeMetaData) {
//			return getMessageMIMEHeaders((MeetingNoticeMetaData)message);
		} else {
			throw new IllegalArgumentException(message.getClass().toString());
		}
	}


	public INotesMessagesMetaData<? extends BaseINotesMessage> getMessagesAndMeetingNoticesMetaData() throws IOException {
		return getMessagesMetaData();
		//FIXME uncomment when we find a way to export meeting invites!
//		if (Folder.INBOX.equals(context.getNotesFolderId()) || Folder.ALL.equals(context.getNotesFolderId())) {
//			INotesMessagesMetaData<MessageMetaData> messagesMetaData = getMessagesMetaData();
//			INotesMessagesMetaData<MeetingNoticeMetaData> noticesMetaData = getMeetingNoticesMetaData();
//			INotesMessagesMetaData<BaseINotesMessage> result = messagesMetaData.clone();
//			result.entries.clear();
//			result.entries.addAll(messagesMetaData.entries);
//			result.entries.addAll(noticesMetaData.entries);
//			Collections.sort(result.entries, new Comparator<BaseINotesMessage>() {
//				@Override
//				public int compare(BaseINotesMessage o1, BaseINotesMessage o2) {
//					return o1.getDate().compareTo(o2.getDate());
//				}
//			});
//			return result;
//		} else {
//			return getMessagesMetaData();
//		}
	}

	public INotesMessagesMetaData<? extends BaseINotesMessage> getMessagesAndMeetingNoticesMetaData(Date oldestMessageToFetch) throws IOException {
		return getMessagesMetaData(oldestMessageToFetch);
		//FIXME uncomment when we find a way to export meeting invites!
//		if (Folder.INBOX.equals(context.getNotesFolderId()) || Folder.ALL.equals(context.getNotesFolderId())) {
//			INotesMessagesMetaData<MessageMetaData> messagesMetaData = getMessagesMetaData(oldestMessageToFetch);
//			INotesMessagesMetaData<MeetingNoticeMetaData> noticesMetaData = getMeetingNoticesMetaData(oldestMessageToFetch);
//			INotesMessagesMetaData<BaseINotesMessage> result = messagesMetaData.clone();
//			result.entries.clear();
//			result.entries.addAll(messagesMetaData.entries);
//			result.entries.addAll(noticesMetaData.entries);
//			Collections.sort(result.entries, new Comparator<BaseINotesMessage>() {
//				@Override
//				public int compare(BaseINotesMessage o1, BaseINotesMessage o2) {
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
				logger.warn("ERROR while logging out user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
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
		private final DateFormat LOTUS_NOTES_BROKEN_DATE_FORMAT;
		private boolean inHeaders = true;

		public HttpCleaningLineIterator(final ClientHttpResponse httpResponse) throws IOException {
			super(new InputStreamReader(httpResponse.getBody(), context.getCharset(httpResponse)));
			this.httpResponse = httpResponse;
			if (context.isFixLotusNotesDateMIMEHeader()) {
				inHeaders = true;
				LOTUS_NOTES_BROKEN_DATE_FORMAT = new SimpleDateFormat("d-MMM-yyyy HH:mm:ss z", Locale.US);
				LOTUS_NOTES_BROKEN_DATE_FORMAT.setLenient(false);
			} else {
				inHeaders = false;
				LOTUS_NOTES_BROKEN_DATE_FORMAT = null;
			}
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
			if (inHeaders && line.startsWith("Date: ") && line.contains("-")) {
				String lineToParse = line;
				try {
					int minutesToAdd = 0;
					// TimeZones not recognized by SimpleDateFormat
					for (Map.Entry<String, Integer> tz : LOTUS_TZ.entrySet()) {
						if (lineToParse.endsWith(tz.getKey())) {
							lineToParse = line.substring(0, line.length() - tz.getKey().length()) + ' '+GMT_ID;
							minutesToAdd = - tz.getValue().intValue();
						}
					}
					Date date = LOTUS_NOTES_BROKEN_DATE_FORMAT.parse(lineToParse.substring("Date: ".length()).trim());
					if (minutesToAdd != 0) {
						date.setTime(date.getTime() + minutesToAdd * DateUtils.MILLIS_PER_MINUTE);
					}
					String rfcLine = "Date: " + fr.cedrik.inotes.util.DateUtils.RFC2822_DATE_TIME_FORMAT.format(date);
					logger.debug("Fixing broken Lotus Date header; before: {}\tafter: {}", line, rfcLine);
					line = rfcLine;
				} catch (ParseException notAnError) {
					logger.debug("Date header OK: {}", line);
				}
			}
			return line;
		}

		@Override
		public void close() {
			super.close();
			httpResponse.close();
		}
	}

	private static final String GMT_ID = "GMT";// TimeZone.GMT_ID
	private static final Map<String, Integer> LOTUS_TZ = new HashMap<String, Integer>();
	// see http://www.ibm.com/developerworks/lotus/library/ls-keeping_time/side1.html
	// see http://tools.ietf.org/html/rfc5322#section-4.3
	static {
//		LOTUS_TZ.put(" GMT", Integer.valueOf(0*60));     // Greenwich Mean Time
		LOTUS_TZ.put(" GDT", Integer.valueOf((0+1)*60));
		LOTUS_TZ.put(" ZW1", Integer.valueOf(-1*60));
		LOTUS_TZ.put(" YW1", Integer.valueOf((-1+1)*60));
		LOTUS_TZ.put(" ZW1", Integer.valueOf(-2*60));
		LOTUS_TZ.put(" YW2", Integer.valueOf((-2+1)*60));
		LOTUS_TZ.put(" ZW3", Integer.valueOf(-3*60));
		LOTUS_TZ.put(" YW3", Integer.valueOf((-3+1)*60));
		LOTUS_TZ.put(" NST", Integer.valueOf(-3*60-30)); // Newfoundland
		LOTUS_TZ.put(" NDT", Integer.valueOf((-3+1)*60-30));
		LOTUS_TZ.put(" AST", Integer.valueOf(-4*60));    // Atlantic Standard Time
		LOTUS_TZ.put(" ADT", Integer.valueOf((-4+1)*60));
		LOTUS_TZ.put(" EST", Integer.valueOf(-5*60));    // Eastern Standard Time
		LOTUS_TZ.put(" EDT", Integer.valueOf((-5+1)*60));
		LOTUS_TZ.put(" CST", Integer.valueOf(-6*60));    // Central Standard Time
		LOTUS_TZ.put(" CDT", Integer.valueOf((-6+1)*60));
		LOTUS_TZ.put(" MST", Integer.valueOf(-7*60));    // Mountain Standard Time
		LOTUS_TZ.put(" MDT", Integer.valueOf((-7+1)*60));
		LOTUS_TZ.put(" PST", Integer.valueOf(-8*60));    // Pacific Standard Time
		LOTUS_TZ.put(" PDT", Integer.valueOf((-8+1)*60));
		LOTUS_TZ.put(" YST", Integer.valueOf(-9*60));    // Alaska Standard Time
		LOTUS_TZ.put(" YDT", Integer.valueOf((-9+1)*60));
		LOTUS_TZ.put(" ZW9B",  Integer.valueOf(-9*60-30));
		LOTUS_TZ.put(" HST",   Integer.valueOf(-10*60)); // Hawaii-Aleutian Standard Time
		LOTUS_TZ.put(" HDT",   Integer.valueOf((-10+1)*60));
		LOTUS_TZ.put(" BST",   Integer.valueOf(-11*60)); // Bering Standard Time
		LOTUS_TZ.put(" BDT",   Integer.valueOf((-11+1)*60));
		LOTUS_TZ.put(" ZW12",  Integer.valueOf(-12*60));
		LOTUS_TZ.put(" ZE12C", Integer.valueOf(12*60+45));
		LOTUS_TZ.put(" ZE12",  Integer.valueOf(12*60));
		LOTUS_TZ.put(" ZE11B", Integer.valueOf(11*60+30));
		LOTUS_TZ.put(" ZE11",  Integer.valueOf(11*60));
		LOTUS_TZ.put(" ZE10B", Integer.valueOf(10*60+30));
		LOTUS_TZ.put(" ZE10",  Integer.valueOf(10*60));
		LOTUS_TZ.put(" ZE9B",  Integer.valueOf(9*60+30));
		LOTUS_TZ.put(" ZE9",   Integer.valueOf(9*60));
		LOTUS_TZ.put(" ZE8",   Integer.valueOf(8*60));
		LOTUS_TZ.put(" ZE7",   Integer.valueOf(7*60));
		LOTUS_TZ.put(" ZE6B",  Integer.valueOf(6*60+30));
		LOTUS_TZ.put(" ZE6",   Integer.valueOf(6*60));
		LOTUS_TZ.put(" ZE5C",  Integer.valueOf(5*60+45));
		LOTUS_TZ.put(" ZE5B",  Integer.valueOf(5*60+30));
		LOTUS_TZ.put(" ZE5",   Integer.valueOf(5*60));
		LOTUS_TZ.put(" ZE4B",  Integer.valueOf(4*60+30));
		LOTUS_TZ.put(" ZE4",   Integer.valueOf(4*60));
		LOTUS_TZ.put(" ZE3B",  Integer.valueOf(3*60+30));
		LOTUS_TZ.put(" ZE3",   Integer.valueOf(3*60));
		LOTUS_TZ.put(" ZE2",   Integer.valueOf(2*60));
		LOTUS_TZ.put(" CET",   Integer.valueOf(1*60));   // Central European Time
		LOTUS_TZ.put(" CEDT",  Integer.valueOf((1+1)*60));
		// remove JVM-known TZ entries
		Iterator<Map.Entry<String, Integer>> iterator = LOTUS_TZ.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Integer> lotus = iterator.next();
			TimeZone tz = TimeZone.getTimeZone(lotus.getKey().trim());
			if (tz != null && ! GMT_ID.equals(tz.getID())) {
//				logger.debug("Removing existing TZ: {}", lotus.getKey());
				iterator.remove();
			}
		}
	}
}
