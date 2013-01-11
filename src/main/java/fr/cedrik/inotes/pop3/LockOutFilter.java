/**
 *
 */
package fr.cedrik.inotes.pop3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide a user + IP lock out mechanism if there are too many failed
 * authentication attempts in a given period of time. To ensure correct
 * operation, there is a reasonable degree of synchronization.
 *
 * Heavily inspired by Tomcat's LockOutRealm :-)
 *
 * @author C&eacute;drik LIME
 */
class LockOutFilter {
	private static final Logger logger = LoggerFactory.getLogger(LockOutFilter.class);

	/**
	 * The number of times in a row a user has to fail authentication to be
	 * locked out. Defaults to 5.
	 */
	protected int failureCount = 5;

	/**
	 * The time (in seconds) a user is locked out for after too many
	 * authentication failures. Defaults to 300 (5 minutes).
	 */
	protected int lockOutTime = 300;

	/**
	 * Number of users that have failed authentication to keep in cache. Over
	 * time the cache will grow to this size and may not shrink. Defaults to
	 * 1000.
	 */
	protected int cacheSize = 1000;

	/**
	 * If a failed user is removed from the cache because the cache is too big
	 * before it has been in the cache for at least this period of time (in
	 * seconds) a warning message will be logged. Defaults to 3600 (1 hour).
	 */
	protected int cacheRemovalWarningTime = 3600;

	/**
	 * Users whose last authentication attempt failed. Entries will be ordered
	 * in access order from least recent to most recent.
	 */
	protected Map<Object,LockRecord> failedUsers = null;


	/**
	 *
	 */
	public LockOutFilter(POP3Properties pop3Properties) {
		super();
		this.failureCount = Integer.parseInt(pop3Properties.getProperty("pop3.lockout.failureCount", Integer.toString(this.failureCount)));
		this.lockOutTime  = Integer.parseInt(pop3Properties.getProperty("pop3.lockout.lockOutTime",  Integer.toString(this.lockOutTime)));
		this.cacheSize    = Integer.parseInt(pop3Properties.getProperty("pop3.lockout.cacheSize",    Integer.toString(this.cacheSize)));
		this.cacheRemovalWarningTime = Integer.parseInt(pop3Properties.getProperty("pop3.lockout.cacheRemovalWarningTime", Integer.toString(this.cacheRemovalWarningTime)));
		failedUsers = new LinkedHashMap<Object, LockRecord>(cacheSize, 0.75f, true) {
			private static final long serialVersionUID = -5927166435897099729L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, LockRecord> eldest) {
				if (size() > cacheSize) {
					// Check to see if this element has been removed too quickly
					long timeInCache =
							(System.currentTimeMillis() - eldest.getValue().getLastFailureTime()) / DateUtils.MILLIS_PER_SECOND;

					if (timeInCache < cacheRemovalWarningTime) {
						logger.warn("\"{}\" was removed from the failed users cache after {} seconds to keep the cache size within the limit set",
								eldest.getKey(), Long.valueOf(timeInCache));
					}
					return true;
				}
				return false;
			}
		};
	}

	/**
	 * Unlock the specified username. This will remove all records of
	 * authentication failures for this user.
	 *
	 * @param username
	 */
	public void unlock(Object username) {
		// Auth success clears the lock record so...
		registerAuthSuccess(username);
	}

	/**
	 * Checks to see if the current user is locked. If this is associated with
	 * a login attempt, then the last access time will be recorded and any
	 * attempt to authenticated a locked user will log a warning.
	 *
	 * @param username
	 * @return
	 */
	public boolean isLocked(Object username) {
		LockRecord lockRecord = null;
		synchronized (this) {
			lockRecord = failedUsers.get(username);
		}

		// No lock record means user can't be locked
		if (lockRecord == null) {
			return false;
		}

		// Check to see if user is locked
		if (lockRecord.getFailures() >= failureCount &&
				(System.currentTimeMillis() - lockRecord.getLastFailureTime()) / DateUtils.MILLIS_PER_SECOND < lockOutTime) {
			return true;
		}

		// User has not, yet, exceeded lock thresholds
		return false;
	}

	/**
	 * After successful authentication, any record of previous authentication
	 * failure is removed.
	 *
	 * @param username
	 */
	public synchronized void registerAuthSuccess(Object username) {
		// Successful authentication means removal from the list of failed users
		failedUsers.remove(username);
	}

	/**
	 * After a failed authentication, add the record of the failed
	 * authentication.
	 *
	 * @param username
	 */
	public void registerAuthFailure(Object username) {
		LockRecord lockRecord = null;
		synchronized (this) {
			if (!failedUsers.containsKey(username)) {
				lockRecord = new LockRecord();
				// Yes those 2 put() are the same. Please leave them alone, as they enable the cache to shrink.
				failedUsers.put(username, lockRecord);
				failedUsers.put(username, lockRecord);
			} else {
				lockRecord = failedUsers.get(username);
				if (lockRecord.getFailures() >= failureCount &&
						((System.currentTimeMillis() - lockRecord.getLastFailureTime()) / DateUtils.MILLIS_PER_SECOND)
								> lockOutTime) {
					// User was previously locked out but lockout has now
					// expired so reset failure count
					lockRecord.setFailures(0);
				}
			}
		}
		lockRecord.registerFailure();
	}

	/***********************************************************************/

	/**
	 * Get the number of failed authentication attempts required to lock the
	 * user account.
	 * @return the failureCount
	 */
	public int getFailureCount() {
		return failureCount;
	}

	/**
	 * Set the number of failed authentication attempts required to lock the
	 * user account.
	 * @param failureCount the failureCount to set
	 */
	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}

	/**
	 * Get the period for which an account will be locked.
	 * @return the lockOutTime
	 */
	public int getLockOutTime() {
		return lockOutTime;
	}

	/**
	 * Set the period for which an account will be locked.
	 * @param lockOutTime the lockOutTime to set
	 */
	public void setLockOutTime(int lockOutTime) {
		this.lockOutTime = lockOutTime;
	}

	/**
	 * Get the maximum number of users for which authentication failure will be
	 * kept in the cache.
	 * @return the cacheSize
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * Set the maximum number of users for which authentication failure will be
	 * kept in the cache.
	 * @param cacheSize the cacheSize to set
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * Get the minimum period a failed authentication must remain in the cache
	 * to avoid generating a warning if it is removed from the cache to make
	 * space for a new entry.
	 * @return the cacheRemovalWarningTime
	 */
	public int getCacheRemovalWarningTime() {
		return cacheRemovalWarningTime;
	}

	/**
	 * Set the minimum period a failed authentication must remain in the cache
	 * to avoid generating a warning if it is removed from the cache to make
	 * space for a new entry.
	 * @param cacheRemovalWarningTime the cacheRemovalWarningTime to set
	 */
	public void setCacheRemovalWarningTime(int cacheRemovalWarningTime) {
		this.cacheRemovalWarningTime = cacheRemovalWarningTime;
	}

	/***********************************************************************/

	protected static class LockRecord {
		private final AtomicInteger failures = new AtomicInteger(0);
		private long lastFailureTime = 0;

		public int getFailures() {
			return failures.get();
		}

		public void setFailures(int theFailures) {
			failures.set(theFailures);
		}

		public long getLastFailureTime() {
			return lastFailureTime;
		}

		public void registerFailure() {
			failures.incrementAndGet();
			lastFailureTime = System.currentTimeMillis();
		}
	}
}
