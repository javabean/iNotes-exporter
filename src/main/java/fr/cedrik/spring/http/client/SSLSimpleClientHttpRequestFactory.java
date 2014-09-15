/**
 *
 */
package fr.cedrik.spring.http.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.springframework.http.client.ClientHttpRequest;

/**
 * Note: does not do authentication; this can be added to the returned {@link ClientHttpRequest}'s headers.
 *
 * @author C&eacute;drik LIME
 */
public class SSLSimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	// Keystore holding private keys (client certificate)
	private String keyStoreName;
	private String keyStorePassword;
	private String keyStoreType     = "PKCS12";
	// Keystore holding public certificate (server certificate)
	private String trustStoreName;
	private String trustStorePassword;
	private String trustStoreType = "PKCS12";

	protected SSLSocketFactory socketFactory;
	protected HostnameVerifier hostnameVerifier;

	/**
	 *
	 */
	public SSLSimpleClientHttpRequestFactory() {
		super();
	}

	@PostConstruct
	protected void initialise() {
		// Builds the SSLSocketFactory
		if (keyStoreName != null && keyStorePassword != null && keyStoreType != null) {
			try {
				// Load KeyStore
				KeyStore keyStore = loadKeyStore(keyStoreName, keyStorePassword, keyStoreType);
				KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance("SunX509");
				keyMgrFactory.init(keyStore, keyStorePassword.toCharArray());

				// Load TrustStore
				TrustManagerFactory trustMgrFactory = null;
				if (trustStoreName != null && trustStorePassword != null && trustStoreType != null) {
					KeyStore trustStore = loadKeyStore(trustStoreName, trustStorePassword, trustStoreType);
					trustMgrFactory = TrustManagerFactory.getInstance("SunX509");
					trustMgrFactory.init(trustStore);
				}

				// create and initialize an SSLContext object
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyMgrFactory.getKeyManagers(),
						(trustMgrFactory != null ? trustMgrFactory.getTrustManagers() : null),
						null);
				// obtain the SSLSocketFactory from the SSLContext
				socketFactory = sslContext.getSocketFactory();
				//SSLSocket theSocket = (SSLSocket) socketFactory.createSocket(host, port);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e.getLocalizedMessage(), e);
			}
		}
	}

	protected KeyStore loadKeyStore(String storeName, String storePassword, String storeType) throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
		KeyStore store = null;
		InputStream is = null;
		try {
			store = KeyStore.getInstance(storeType);
			try {
				is = new FileInputStream(storeName);
			} catch (FileNotFoundException fnfe) {
				if (Thread.currentThread().getContextClassLoader() != null) {
					is = Thread.currentThread().getContextClassLoader().getResourceAsStream(storeName);
					if (is == null) {
						throw fnfe;
					}
				} else {
					throw fnfe;
				}
			}
			store.load(is, storePassword.toCharArray());
		} catch (IOException e) {
			throw new RuntimeException(e.getLocalizedMessage(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return store;
	}

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		super.prepareConnection(connection, httpMethod);
//		connection.addRequestProperty("Authorization", BASIC_AUTH + Base64.encode("user name" + ':' + "pass phrase"));
		if (connection instanceof HttpsURLConnection) {
			if (socketFactory != null) {
				((HttpsURLConnection)connection).setSSLSocketFactory(socketFactory);
			}
			if (hostnameVerifier != null) {
				((HttpsURLConnection)connection).setHostnameVerifier(hostnameVerifier);
			}
		}
	}


	/*
	 * High-level accessors: KeyStore and TrustStore
	 */

	public void setKeyStoreName(String keyStoreName) {
		this.keyStoreName = keyStoreName;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	/**
	 * @see KeyStore#getInstance(String)
	 */
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public void setTrustStoreName(String trustStoreName) {
		this.trustStoreName = trustStoreName;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	/**
	 * @see KeyStore#getInstance(String)
	 */
	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	/*
	 * Low-level accessors: SSLSocketFactory and HostnameVerifier
	 */

	public SSLSocketFactory getSocketFactory() {
		return socketFactory;
	}

	public void setSocketFactory(SSLSocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public HostnameVerifier getHostnameVerifier() {
		return hostnameVerifier;
	}

	public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.hostnameVerifier = hostnameVerifier;
	}
}
