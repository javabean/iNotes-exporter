/**
 *
 */
package fr.cedrik.spring.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * @author C&eacute;drik LIME
 */
class GZipDecodingClientHttpRequest implements ClientHttpRequest {

	private final ClientHttpRequest request;

	/**
	 *
	 */
	public GZipDecodingClientHttpRequest(ClientHttpRequest request) {
		super();
		this.request = request;
	}

	/** {@inheritDoc} */
	@Override
	public HttpMethod getMethod() {
		return request.getMethod();
	}

	/** {@inheritDoc} */
	@Override
	public URI getURI() {
		return request.getURI();
	}

	/** {@inheritDoc} */
	@Override
	public HttpHeaders getHeaders() {
		return request.getHeaders();
	}

	/** {@inheritDoc} */
	@Override
	public OutputStream getBody() throws IOException {
		return request.getBody();
	}

	/** {@inheritDoc} */
	@Override
	public ClientHttpResponse execute() throws IOException {
		return new GZipDecodingClientHttpResponse(request.execute());
	}

}
