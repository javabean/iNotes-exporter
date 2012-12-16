/**
 *
 */
package fr.cedrik.spring.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

/**
 * @author C&eacute;drik LIME
 */
public class GZipDecodingClientHttpResponse implements ClientHttpResponse {
	private static final String CONTENT_ENCODING = "Content-Encoding";//$NON-NLS-1$

	private final ClientHttpResponse response;

	/**
	 *
	 */
	public GZipDecodingClientHttpResponse(ClientHttpResponse response) {
		super();
		this.response = response;
	}

	/** {@inheritDoc} */
	@Override
	public InputStream getBody() throws IOException {
		final String encoding = response.getHeaders().getFirst(CONTENT_ENCODING);
		if ("gzip".equalsIgnoreCase(encoding)) {//$NON-NLS-1$
			return new GZIPInputStream(response.getBody());
		} else if ("deflate".equalsIgnoreCase(encoding)) {//$NON-NLS-1$
			return new InflaterInputStream(response.getBody(), new Inflater(true));
		} else {
			return response.getBody();
		}
	}

	/** {@inheritDoc} */
	@Override
	public HttpHeaders getHeaders() {
		return this.response.getHeaders();
	}

	/** {@inheritDoc} */
	@Override
	public HttpStatus getStatusCode() throws IOException {
		return this.response.getStatusCode();
	}

	/** {@inheritDoc} */
	@Override
	public int getRawStatusCode() throws IOException {
		return this.response.getRawStatusCode();
	}

	/** {@inheritDoc} */
	@Override
	public String getStatusText() throws IOException {
		return this.response.getStatusText();
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		this.response.close();
	}

}
