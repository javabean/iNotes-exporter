/**
 *
 */
package fr.cedrik.spring.http.client;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpAccessor;

import fr.cedrik.inotes.util.Charsets;


/**
 * Strategy interface for actual execution of an HTTP request.
 *
 * <p>Two implementations are provided out of the box (see {@link #setRequestFactory(org.springframework.http.client.ClientHttpRequestFactory)}):
 * <ul>
 * <li><b>SimpleClientHttpRequestFactory or its subclass SSLSimpleClientHttpRequestFactory:</b>
 * Uses Java SE facilities to execute HTTP requests, without support
 * for HTTP authentication or advanced configuration options.
 * <li><b>CommonsClientHttpRequestFactory:</b>
 * Uses Jakarta's Commons HttpClient to execute HTTP requests,
 * allowing to use a preconfigured HttpClient instance
 * (potentially with authentication, HTTP connection pooling, etc).
 * </ul>
 *
 * @author C&eacute;drik LIME
 */
public class HttpRequestExecutor extends HttpAccessor {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 *
	 */
	public HttpRequestExecutor() {
		super();
		setRequestFactory(new SimpleClientHttpRequestFactory());// FIXME delete when https://jira.springframework.org/browse/SPR-7743 is closed
	}


	/**
	 * Factory method to create a new {@link ClientHttpRequest} via this template's {@link ClientHttpRequestFactory}
	 * for the specified {@code url} and HTTP {@code method}, eventually preemptively authenticating (HTTP BASIC) with {@code credentials},
	 * appending optional {@code extraParameters} and HTTP {@code extraHeaders}.
	 *
	 * @param url  the URL to connect to
	 * @param method  the HTTP method to exectute (GET, POST, etc.)
	 * @param extraParameters
	 * @param extraHttpHeaders
	 * @return the created request
	 * @throws IOException  in case of I/O errors
	 */
	public ClientHttpRequest createRequest(URL url, HttpMethod method, Map<String, ?> extraParameters, Map<String, List<String>> extraHttpHeaders) throws IOException {
		if (method == null) {
			method = HttpMethod.POST;
		}
		// add query parameters to URL
		switch (method) {
			case GET:
			case HEAD:
			case OPTIONS:
			case DELETE:
			case TRACE:
				url = addQueryParameters(url, extraParameters);
				break;
			case POST:
			case PUT:
				// will add form parameters in request body
				break;
			default:
				throw new UnsupportedOperationException(method.toString());
		}
		ClientHttpRequest httpRequest;
		try {
			httpRequest = getRequestFactory().createRequest(url.toURI(), method);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		if (extraHttpHeaders != null && ! extraHttpHeaders.isEmpty()) {
			httpRequest.getHeaders().putAll(extraHttpHeaders);
		}
		// add query parameters to HTTP request body
		if (extraParameters != null && !extraParameters.isEmpty()) {
			switch (method) {
				case GET:
				case HEAD:
				case OPTIONS:
				case DELETE:
				case TRACE:
					// already done
					break;
				case POST:
				case PUT:
				// add form parameters in request body
					StringBuilder postParameters = buildQueryParameters(extraParameters);
					PrintStream body = new PrintStream(httpRequest.getBody(), true, Charsets.UTF_8.name());
					body.append(postParameters.toString());
					body.flush();
					break;
				default:
					throw new UnsupportedOperationException(method.toString());
			}
		}

		return httpRequest;
	}


	/**
	 * Appends the {@code params} parameters to the given {@code url}
	 * @param url
	 * @param params
	 * @return
	 */
	public URL addQueryParameters(URL url, Map<String, ?> params) {
		if (params == null || params.isEmpty()) {
			return url;
		}

		String file = url.getFile();// includes original url query
		if (url.getQuery() == null) {
			file += '?';
		} else {
			file += '&';
		}
		StringBuilder extraQuery = buildQueryParameters(params);
		if (url.getRef() != null) {
			extraQuery.append('#').append(url.getRef());
		}
		file += extraQuery.toString();
		try {
			return new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
		} catch (MalformedURLException mue) {
			throw new AssertionError(mue);
		}
	}

	/**
	 *
	 * @param params
	 * @return GET query parameters, as {@literal application/x-www-form-urlencoded}
	 */
	public StringBuilder buildQueryParameters(Map<String, ?> params) {
		StringBuilder extraQuery = new StringBuilder(Math.max(32, params.size()*8));
		for (Map.Entry<String, ?> entry : params.entrySet()) {
			if (entry.getValue() != null) {
				// Don't append a parameter for a {@code null} value
				try {
					extraQuery
					.append(URLEncoder.encode(entry.getKey(), Charsets.UTF_8.name()))
					.append('=')
					.append(URLEncoder.encode(ObjectUtils.toString(entry.getValue()), Charsets.UTF_8.name()))
					.append('&');
				} catch (UnsupportedEncodingException uee) {
					throw new AssertionError(uee);
				}
			}
		}
		// remove trailing '&'
		if (extraQuery.length() > 0) {
			extraQuery.deleteCharAt(extraQuery.length()-1);
		}
		return extraQuery;
	}

	private static final Charset DEFAULT_HTTP_ENCODING = Charsets.ISO_8859_1;
	/**
	 * @param httpResponse
	 * @return encoding of HTTP response body
	 */
	public Charset getCharset(ClientHttpResponse httpResponse) {
		Charset encoding = DEFAULT_HTTP_ENCODING;
		if (httpResponse.getHeaders().getContentType() != null) {
			Charset charSet = httpResponse.getHeaders().getContentType().getCharSet();
			if (charSet != null) {
				encoding = charSet;
			}
		}
		return encoding;
	}

}
