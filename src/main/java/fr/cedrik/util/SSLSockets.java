/**
 *
 */
package fr.cedrik.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author C&eacute;drik LIME
 */
public class SSLSockets {

	private SSLSockets() {
		assert false;
	}

	/**
	 * keyStore: Keystore holding private keys (client certificate)
	 * trustStore: Keystore holding public certificate (server certificate)
	 */
	public static SSLSocketFactory getSSLSocketFactory(String keyStoreName, String keyStorePassword, String keyStoreType, String keyPassword,
			String trustStoreName, String trustStorePassword, String trustStoreType) {
		// Builds the SSLSocketFactory
		if (StringUtils.isNotBlank(keyStoreName)) {
			try {
				// create and initialize an SSLContext object
				SSLContext sslContext = getSSLContext(keyStoreName, keyStorePassword, keyStoreType, keyPassword,
						trustStoreName, trustStorePassword, trustStoreType);
				// obtain the SSLSocketFactory from the SSLContext
				SSLSocketFactory socketFactory = sslContext.getSocketFactory();
				//SSLSocket theSocket = (SSLSocket) socketFactory.createSocket(host, port);
				return socketFactory;
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e.getLocalizedMessage(), e);
			}
		} else {
			return (SSLSocketFactory) SSLSocketFactory.getDefault();
		}
	}

	/**
	 * keyStore: Keystore holding private keys (client certificate)
	 * trustStore: Keystore holding public certificate (server certificate)
	 */
	public static SSLServerSocketFactory getSSLServerSocketFactory(String keyStoreName, String keyStorePassword, String keyStoreType, String keyPassword,
			String trustStoreName, String trustStorePassword, String trustStoreType) {
		// Builds the SSLSocketFactory
		if (StringUtils.isNotBlank(keyStoreName)) {
			try {
				// create and initialize an SSLContext object
				SSLContext sslContext = getSSLContext(keyStoreName, keyStorePassword, keyStoreType, keyPassword,
						trustStoreName, trustStorePassword, trustStoreType);
				// obtain the SSLSocketFactory from the SSLContext
				SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
				//SSLServerSocket theSocket = (SSLServerSocket) socketFactory.createServerSocket(host, port);
				return socketFactory;
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e.getLocalizedMessage(), e);
			}
		} else {
			return (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		}
	}

	/**
	 * keyStore: Keystore holding private keys (client certificate)
	 * trustStore: Keystore holding public certificate (server certificate)
	 */
	public static SSLContext getSSLContext(String keyStoreName, String keyStorePassword, String keyStoreType, String keyPassword,
			String trustStoreName, String trustStorePassword, String trustStoreType)
			throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, CertificateException, KeyManagementException {

		// Builds the SSLSocketFactory
		if (StringUtils.isNotBlank(keyStoreName)) {
			// Load KeyStore
			KeyStore keyStore = loadKeyStore(keyStoreName, keyStorePassword, keyStoreType);
			KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance("SunX509");//$NON-NLS-1$
			keyMgrFactory.init(keyStore, keyPassword != null ? keyPassword.toCharArray() : null);

			// Load TrustStore
			TrustManagerFactory trustMgrFactory = null;
			if (StringUtils.isNotBlank(trustStoreName)) {
				KeyStore trustStore = loadKeyStore(trustStoreName, trustStorePassword, trustStoreType);
				trustMgrFactory = TrustManagerFactory.getInstance("SunX509");//$NON-NLS-1$
				trustMgrFactory.init(trustStore);
			}

			// create and initialize an SSLContext object
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyMgrFactory.getKeyManagers(),
					(trustMgrFactory != null ? trustMgrFactory.getTrustManagers() : null),
					null);
			return sslContext;
		} else {
			return SSLContext.getDefault();
		}
	}

	public static KeyStore loadKeyStore(String storeName, String storePassword, String storeType) throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
		if (StringUtils.isBlank(storeType)) {
			storeType = "PKCS12";//$NON-NLS-1$
		}
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
			store.load(is, storePassword != null ? storePassword.toCharArray() : null);
		} catch (IOException e) {
			throw new RuntimeException(e.getLocalizedMessage(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return store;
	}

	/**
	 * all-trusting trust manager, used for debugging purposes (e.g. connecting to a secure socket via a proxy); hence the {@code @Deprecated}.<br />
	 * Note that for non-debugging purposes, you should very much get the remote public certificate, and load it via {@link #getSSLSocketFactory(String, String, String, String, String, String, String)}!
	 */
	@Deprecated
	public static final SSLSocketFactory trustingSSLSocketFactory;
	static {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				@Override
				public void checkServerTrusted(X509Certificate[] certs,String authType) {
				}
			} };
			SSLContext sc = SSLContext.getInstance("TLS");//$NON-NLS-1$
			sc.init(null, trustAllCerts, new SecureRandom());
			trustingSSLSocketFactory = sc.getSocketFactory();
		} catch (KeyManagementException e) {
			throw new RuntimeException("Can not initialize trustingSSLSocketFactory", e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Can not initialize trustingSSLSocketFactory", e);
		}
	}

	/**
	 * all-trusting host name verifier, used for debugging purposes (e.g. connecting to a secure socket via a proxy); hence the {@code @Deprecated}
	 */
	@Deprecated
	public static final HostnameVerifier allHostsValid = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

}
