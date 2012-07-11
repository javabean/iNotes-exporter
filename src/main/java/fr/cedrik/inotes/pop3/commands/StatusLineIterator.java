/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.io.LineIterator;

/**
 * {@link LineIterator} with an added first line
 *
 * @author C&eacute;drik LIME
 */
class StatusLineIterator implements Iterator<String>, Closeable {
	private String firstLine;
	private final Iterator<String> delegate;

	/**
	 * @throws IllegalArgumentException
	 */
	public StatusLineIterator(String firstLine, Iterator<String> delegate) throws IllegalArgumentException {
		super();
		this.firstLine = firstLine;
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		if (firstLine != null) {
			return true;
		} else if (delegate != null) {
			return delegate.hasNext();
		} else {
			return false;
		}
	}

	@Override
	public String next() {
		if (firstLine != null) {
			String line = firstLine;
			firstLine = null;
			return line;
		} else if (delegate != null) {
			return delegate.next();
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * @see LineIterator#close()
	 */
	@Override
	public void close() throws IOException {
		if (delegate != null) {
			if (delegate instanceof LineIterator) {
				((LineIterator)delegate).close();
			} else if (delegate instanceof Closeable) {
				((Closeable)delegate).close();
			}
		}
	}

	/**
	 * Unsupported.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove unsupported on StatusLineIterator");
	}
}
