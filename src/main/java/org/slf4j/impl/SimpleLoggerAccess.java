/**
 */
package org.slf4j.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * @author C&eacute;drik LIME
 */
public class SimpleLoggerAccess {

//	final static SimpleLoggerFactory INSTANCE = SimpleLoggerFactory.INSTANCE;
	final static SimpleLoggerFactory INSTANCE = (SimpleLoggerFactory) StaticLoggerBinder.getSingleton().getLoggerFactory();

	private SimpleLoggerAccess() {
	}

	private static final int LOG_LEVEL_ALL = LocationAwareLogger.TRACE_INT - 10;//SimpleLogger.LOG_LEVEL_ALL;
	private static final int LOG_LEVEL_OFF = LocationAwareLogger.ERROR_INT + 10;//SimpleLogger.LOG_LEVEL_OFF;

	/**
	 * Return an appropriate {@link SimpleLogger} instance by name, or
	 * {@code null} if none exists.
	 *
	 * @see ILoggerFactory#getLogger(String)
	 */
	public static SimpleLogger getLogger(String name) {
		// protect against concurrent access of the loggerMap
//		synchronized (INSTANCE) { // Note: we don't need to synchronise since SLF4J 1.7.5
			SimpleLogger slogger = (SimpleLogger) INSTANCE.loggerMap.get(name);
			return slogger;
//		}
	}

	public static Collection<SimpleLogger> getAllLoggers() {
		// protect against concurrent access of the loggerMap
//		synchronized (INSTANCE) { // Note: we don't need to synchronise since SLF4J 1.7.5
			return new ArrayList<SimpleLogger>(Collection.class.cast(INSTANCE.loggerMap.values()));
//		}
	}


	public static void changeLoggerLevel(String name, String newLevel) throws IllegalArgumentException {
		changeLoggerLevel(getLogger(name), levelNameToInt(newLevel));
	}

	public static void changeLoggerLevel(SimpleLogger logger, String newLevel) throws IllegalArgumentException {
		changeLoggerLevel(logger, levelNameToInt(newLevel));
	}

	public static void changeLoggerLevel(SimpleLogger logger, int newLevel) throws IllegalArgumentException {
		if (logger == null) {
			return;
		}
		// Sanity check; will throw IllegalArgumentException
		getLevelName(newLevel);
		logger.currentLogLevel = newLevel;
	}


	public static int levelNameToInt(String lvl) throws IllegalArgumentException {
		if ("all".equalsIgnoreCase(lvl)) {
			return LOG_LEVEL_ALL;
		} else if ("trace".equalsIgnoreCase(lvl)) {
			return LocationAwareLogger.TRACE_INT;
		} else if ("debug".equalsIgnoreCase(lvl)) {
			return LocationAwareLogger.DEBUG_INT;
		} else if ("info".equalsIgnoreCase(lvl)) {
			return LocationAwareLogger.INFO_INT;
		} else if ("warn".equalsIgnoreCase(lvl)) {
			return LocationAwareLogger.WARN_INT;
		} else if ("error".equalsIgnoreCase(lvl)) {
			return LocationAwareLogger.ERROR_INT;
//		} else if("fatal".equalsIgnoreCase(lvl)) {
//			return SimpleLogger.LOG_LEVEL_FATAL;
		} else if ("off".equalsIgnoreCase(lvl)) {
			return LOG_LEVEL_OFF;
		} else {
			throw new IllegalArgumentException(lvl);
		}
	}

	public static String getLevelName(SimpleLogger logger) throws IllegalArgumentException {
		return getLevelName(logger.currentLogLevel);
	}
	public static String getLevelName(int level) throws IllegalArgumentException {
	    switch(level) {
	      case LOG_LEVEL_ALL: return "ALL";
	      case LocationAwareLogger.TRACE_INT: return "TRACE";
	      case LocationAwareLogger.DEBUG_INT: return "DEBUG";
	      case LocationAwareLogger.INFO_INT:  return "INFO";
	      case LocationAwareLogger.WARN_INT:  return "WARN";
	      case LocationAwareLogger.ERROR_INT: return "ERROR";
//	      case SimpleLogger.LOG_LEVEL_FATAL: return "FATAL";
	      case LOG_LEVEL_OFF: return "OFF";
	      default: throw new IllegalArgumentException(Integer.toString(level));
	    }
	}
}
