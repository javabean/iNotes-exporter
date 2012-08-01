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

import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class Session {
	private static final int META_DATA_LOAD_BATCH_SIZE = 500;

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected final HttpContext context;
	protected final Set<String> toDelete = new HashSet<String>();
	protected final Set<String> toMarkUnread = new HashSet<String>();
	protected final Set<String> toMarkRead = new HashSet<String>();
	protected final Set<String> toMarkUnreadAll = new HashSet<String>();
	protected final Set<String> toMarkReadAll = new HashSet<String>();
	protected MessagesMetaData allMessagesCache = null;
	protected boolean isLoggedIn = false;
	protected List<Folder> folders = new ArrayList<Folder>();

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

	public void setServerAddress(String url) {
		if (isLoggedIn) {
			throw new IllegalStateException();
		}
		context.iNotes.setServerAddress(url);
	}

	public List<Folder> getFolders() {
		if (! isLoggedIn) {
			throw new IllegalStateException();
		}
		return folders;
	}

	public void setCurrentFolder(Folder folder) throws IOException {
		if (isLoggedIn) {
			cleanup();
		}
		context.setNotesFolderName(folder.id);
		allMessagesCache = null;
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
//			context.getHttpHeaders().set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2");
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

			// play the SessionInfo URL to parse the folders
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

	public MessagesMetaData getMessagesMetaData() throws IOException {
		if (allMessagesCache != null) {
			return allMessagesCache;
		}
		allMessagesCache = getMessagesMetaData(null);
		return allMessagesCache;
	}
	public MessagesMetaData getMessagesMetaData(Date oldestMessageToFetch) throws IOException {
		checkLoggedIn();
		if (oldestMessageToFetch == null) {
			oldestMessageToFetch = new Date(0);
		}
		// iNotes limits the number of results to 1000. Need to paginate.
		int start = 1;
		MessagesMetaData messages = null, partialMessages;
		boolean stopLoading = false;
		do {
			partialMessages = getMessagesMetaDataNoSort(start, META_DATA_LOAD_BATCH_SIZE);
			if (messages == null) {
				messages = partialMessages;
				// filter on date
				Iterator<MessageMetaData> iterator = messages.entries.iterator();
				while (iterator.hasNext()) {
					MessageMetaData message = iterator.next();
					if (message.date.before(oldestMessageToFetch)) {
						iterator.remove();
					}
				}
			} else {
				for (MessageMetaData message : partialMessages.entries) {
					if (message.date.before(oldestMessageToFetch)) {
						stopLoading = true;
						break;
					}
					messages.entries.add(message);
				}
			}
			start += META_DATA_LOAD_BATCH_SIZE;
		} while (! stopLoading && partialMessages.entries.size() >= META_DATA_LOAD_BATCH_SIZE);
		Collections.reverse(messages.entries);
		logger.trace("Loaded {} messages metadata", Integer.valueOf(messages.entries.size()));
		return messages;
	}

	/**
	 * @return set of messages meta-data, in iNotes order (most recent first)
	 */
	protected MessagesMetaData getMessagesMetaDataNoSort(int start, int count) throws IOException {
		checkLoggedIn();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("charset", CharEncoding.UTF_8);
		params.put("Form", "s_ReadViewEntries");
//		params.put("PresetFields", "DBQuotaInfo;1,FolderName;"+context.getNotesFolderName()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1");
		params.put("TZType", "UTC");
		params.put("Start", Integer.toString(start));
		params.put("Count", Integer.toString(count));
		params.put("resortdescending", "5");
		ClientHttpRequest httpRequest = context.createRequest(new URL(context.getProxyBaseURL()+"&PresetFields=DBQuotaInfo;1,FolderName;"+context.getNotesFolderName()+",UnreadCountInfo;1,s_UsingHttps;1,hc;$98,noPI;1"), HttpMethod.GET, params);
		ClientHttpResponse httpResponse = httpRequest.execute();
		trace(httpRequest, httpResponse);
//		traceBody(httpResponse);// DEBUG
		if (! httpResponse.getStatusCode().series().equals(HttpStatus.Series.SUCCESSFUL)) {
			logger.error("Unknown server response while fetching messages meta-data for user \""+context.getUserName()+"\": " + httpResponse.getStatusCode() + ' ' + httpResponse.getStatusText());
			httpResponse.close();
			return null;
		}
		MessagesMetaData messages;
		try {
			messages = new XMLConverter().convertXML(httpResponse.getBody());
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
			toMarkUnread.add(message.unid);
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
			toMarkUnread.add(message.unid);
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

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void deleteMessage(MessageMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MessageMetaData message : messages) {
			toDelete.add(message.unid);
			toMarkRead.remove(message.unid);
			toMarkUnread.remove(message.unid);
		}
	}

	public void undeleteAllMessages() {
		checkLoggedIn();
		toDelete.clear();
		toMarkRead.clear();
		toMarkRead.addAll(toMarkReadAll);
		toMarkUnread.clear();
		toMarkUnread.addAll(toMarkUnreadAll);
	}

	protected void doDeleteMessages() throws IOException {
		if (toDelete.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_AllDocs", "");
		params.put("h_FolderStorage", "");
		params.put("s_ViewName", context.getNotesFolderName());
		params.put("h_SetCommand", "h_DeletePages");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", StringUtils.join(toDelete, ';'));
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
		logger.info("Deleted (moved to Trash) {} messsage(s): {}", toDelete.size(), StringUtils.join(toDelete, ';'));
		toMarkReadAll.removeAll(toDelete);
		toMarkUnreadAll.removeAll(toDelete);
		toDelete.clear();
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void markMessagesRead(MessageMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MessageMetaData message : messages) {
			toMarkUnreadAll.remove(message.unid);
			toMarkReadAll.add(message.unid);
			if (! toDelete.contains(message.unid)) {
				toMarkUnread.remove(message.unid);
				toMarkRead.add(message.unid);
			}
		}
	}

	protected void doMarkMessagesRead() throws IOException {
		if (toMarkRead.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
		params.put("s_ViewName", context.getNotesFolderName());
		params.put("h_AllDocs", "");
		params.put("h_SetCommand", "h_ShimmerMarkRead");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", StringUtils.join(toMarkRead, ';'));
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
		logger.info("Marked {} messsage(s) as read: {}", toMarkRead.size(), StringUtils.join(toMarkRead, ';'));
		toMarkReadAll.removeAll(toMarkRead);
		toMarkRead.clear();
	}

	/**
	 * will be done server-side on logout
	 * @param messages
	 * @throws IOException
	 */
	public void markMessagesUnread(MessageMetaData... messages) throws IOException {
		checkLoggedIn();
		for (MessageMetaData message : messages) {
			toMarkReadAll.remove(message.unid);
			toMarkUnreadAll.add(message.unid);
			if (! toDelete.contains(message.unid)) {
				toMarkRead.remove(message.unid);
				toMarkUnread.add(message.unid);
			}
		}
	}

	protected void doMarkMessagesUnread() throws IOException {
		if (toMarkUnread.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("Form", "l_HaikuErrorStatusJSON");
		params.put("ui", "dwa_form");
//		params.put("PresetFields", "s_NoMarkRead;1");
		params.put("s_ViewName", context.getNotesFolderName());
		params.put("h_AllDocs", "");
		params.put("h_SetCommand", "h_ShimmerMarkUnread");
		params.put("h_SetReturnURL", "[[./&Form=s_CallBlankScript]]");
		params.put("h_EditAction", "h_Next");
		params.put("h_SetEditNextScene", "l_HaikuErrorStatusJSON");
		params.put("h_SetDeleteList", StringUtils.join(toMarkUnread, ';'));
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
		logger.info("Marked {} messsage(s) as unread: {}", toMarkUnread.size(), StringUtils.join(toMarkUnread, ';'));
		toMarkUnreadAll.removeAll(toMarkUnread);
		toMarkUnread.clear();
	}

	protected void cleanup() throws IOException {
		// do mark messages unread
		doMarkMessagesUnread();
		// do mark messages read
		doMarkMessagesRead();
		// do delete messages
		doDeleteMessages();
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
		toDelete.clear();
		toMarkRead.clear();
		toMarkReadAll.clear();
		toMarkUnread.clear();
		toMarkUnreadAll.clear();
		return true;
	}


	private class HttpCleaningLineIterator extends LineIterator implements Iterator<String>, Closeable {
		private final ClientHttpResponse httpResponse;
		public HttpCleaningLineIterator(final ClientHttpResponse httpResponse) throws IOException {
			super(new InputStreamReader(httpResponse.getBody(), context.getCharset(httpResponse)));
			this.httpResponse = httpResponse;
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
			return line;
		}
		@Override
		public void close() {
			super.close();
			httpResponse.close();
		}
	}
}
