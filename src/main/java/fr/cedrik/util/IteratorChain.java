/**
 *
 */
package fr.cedrik.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.LineIterator;

// Very heavily adapted from commons-collections 3.2.1
/**
 * An IteratorChain is an Iterator that wraps a number of Iterators.
 * <p>
 * This class makes multiple iterators look like one to the caller
 * When any method from the Iterator interface is called, the IteratorChain
 * will delegate to a single underlying Iterator. The IteratorChain will
 * invoke the Iterators in sequence until all Iterators are exhausted.
 * <p>
 * Under many circumstances, linking Iterators together in this manner is
 * more efficient (and convenient) than reading out the contents of each
 * Iterator into a List and creating a new Iterator.
 * <p>
 * NOTE: IteratorChain may contain no
 * iterators. In this case the class will function as an empty iterator.
 *
 * @author Morgan Delagrange
 * @author Stephen Colebourne
 * @author C&eacute;drik LIME
 * @see LineIterator
 */
public class IteratorChain<E> implements Iterator<E>, Closeable {

	/** The chain of iterators */
	protected final List<Iterator<E>> delegates = new ArrayList<Iterator<E>>();
	/** The index of the current iterator */
	protected int currentIteratorIndex = 0;
	/** The current iterator */
	protected Iterator<E> currentIterator = null;
	/**
	 * The "last used" Iterator is the Iterator upon which
	 * next() or hasNext() was most recently called
	 * used for the remove() operation only
	 */
	protected Iterator<E> lastUsedIterator = null;
	/**
	 * Maximum number of elements to return (cap)
	 */
	protected long maxElementsCap = Long.MAX_VALUE;
	/**
	 * Current number of elements served
	 */
	protected long count = 0;

	//-----------------------------------------------------------------------
	/**
	 * Construct an IteratorChain with no Iterators.
	 */
	public IteratorChain() {
		super();
	}

	/**
	 * Construct an IteratorChain with a first {@code E} and Iterators.
	 *
	 * @param firstElement first {@code E} in the IteratorChain
	 */
	public IteratorChain(E firstElement) {
		super();
		List<E> firstElementList = new ArrayList<E>(1);
		firstElementList.add(firstElement);
		addIterator(firstElementList.iterator());
	}

	/**
	 * Construct an IteratorChain with a list of {@code E}.
	 *
	 * @param elements first Iterator in the IteratorChain
	 */
	public IteratorChain(E... elements) {
		super();
		List<E> elementsList = new ArrayList<E>(elements.length);
		for (E e : elements) {
			elementsList.add(e);
		}
		addIterator(elementsList.iterator());
	}

	/**
	 * Construct an IteratorChain with a first {@code E} and Iterators.
	 *
	 * @param firstElement first {@code E} in the IteratorChain
	 * @param delegates  the array of iterators
	 */
	public IteratorChain(E firstElement, Iterator<E>... delegates) {
		super();
		List<E> firstElementList = new ArrayList<E>(1);
		firstElementList.add(firstElement);
		addIterator(firstElementList.iterator());
		for (Iterator<E> delegate : delegates) {
			addIterator(delegate);
		}
	}

	/**
	 * Constructs a new <code>IteratorChain</code> over the array
	 * of iterators.
	 *
	 * @param delegates  the array of iterators
	 * @throws NullPointerException if iterators array is {@code null}
	 */
	public IteratorChain(Iterator<E>... delegates) {
		super();
		for (Iterator<E> delegate : delegates) {
			addIterator(delegate);
		}
	}

	//-----------------------------------------------------------------------

	/**
	 * Add an Iterator to the end of the chain
	 *
	 * @param iterator Iterator to add
	 * @throws IllegalStateException if I've already started iterating
	 */
	protected void addIterator(Iterator<E> iterator) {
		if (iterator != null) {
			delegates.add(iterator);
		}
	}

	/**
	 * Updates the current iterator field to ensure that the current Iterator
	 * is not exhausted
	 */
	protected void updateCurrentIterator() {
		if (currentIterator == null) {
			if (delegates.isEmpty()) {
				currentIterator = EmptyIterator.INSTANCE;
			} else {
				currentIterator = delegates.get(0);
			}
			// set last used iterator here, in case the user calls remove
			// before calling hasNext() or next() (although they shouldn't)
			lastUsedIterator = currentIterator;
		}

		while (currentIterator.hasNext() == false && currentIteratorIndex < delegates.size() - 1) {
			++currentIteratorIndex;
			currentIterator = delegates.get(currentIteratorIndex);
		}
	}

	public void setMaxElementsCap(long maxElementsCap) {
		this.maxElementsCap = maxElementsCap;
	}

	//-----------------------------------------------------------------------

	/**
	 * Return true if any Iterator in the IteratorChain has a remaining element.
	 *
	 * @return true if elements remain
	 */
	@Override
	public boolean hasNext() {
		updateCurrentIterator();
		lastUsedIterator = currentIterator;

		return count < maxElementsCap && currentIterator.hasNext();
	}

	/**
	 * Returns the next Object of the current Iterator
	 *
	 * @return Object from the current Iterator
	 * @throws java.util.NoSuchElementException if all the Iterators are exhausted
	 */
	@Override
	public E next() {
		updateCurrentIterator();
		lastUsedIterator = currentIterator;

		if (count >= maxElementsCap) {
			throw new NoSuchElementException("Served " + count + " elements already");
		}
		++count;
		return currentIterator.next();
	}

	/**
	 * Removes from the underlying collection the last element
	 * returned by the Iterator.  As with next() and hasNext(),
	 * this method calls remove() on the underlying Iterator.
	 * Therefore, this method may throw an
	 * UnsupportedOperationException if the underlying
	 * Iterator does not support this method.
	 *
	 * @throws UnsupportedOperationException
	 *   if the remove operator is not supported by the underlying Iterator
	 * @throws IllegalStateException
	 *   if the next method has not yet been called, or the remove method has
	 *   already been called after the last call to the next method.
	 */
	@Override
	public void remove() {
		if (currentIterator == null) {
			updateCurrentIterator();
		}
		lastUsedIterator.remove();
	}

	@Override
	public void close() throws IOException {
		for (Iterator<E> delegate : delegates) {
			if (delegate instanceof Closeable) {
				((Closeable)delegate).close();
			} else if (delegate instanceof LineIterator) {
				((LineIterator)delegate).close();
			}
		}
	}

	//-----------------------------------------------------------------------

	/**
	 * Provides an implementation of an empty iterator.
	 * <p>
	 * This class provides an implementation of an empty iterator.
	 * This class provides for binary compatability between Commons Collections
	 * 2.1.1 and 3.1 due to issues with <code>IteratorUtils</code>.
	 *
	 * @since Commons Collections 2.1.1 and 3.1
	 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
	 *
	 * @author Stephen Colebourne
	 */
	public static class EmptyIterator<E> implements Iterator<E> {

		/**
		 * Singleton instance of the iterator.
		 * @since Commons Collections 2.1.1 and 3.1
		 */
		public static final Iterator INSTANCE = new EmptyIterator();

		/**
		 * Constructor.
		 */
		protected EmptyIterator() {
			super();
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			throw new NoSuchElementException("Iterator contains no elements");
		}

		@Override
		public void remove() {
			throw new IllegalStateException("Iterator contains no elements");
		}
	}

}
