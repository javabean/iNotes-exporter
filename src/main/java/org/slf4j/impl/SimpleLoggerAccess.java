/**
 */
package org.slf4j.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.ILoggerFactory;

/**
 * @author C&eacute;drik LIME
 */
public class SimpleLoggerAccess {

//	final static SimpleLoggerFactory INSTANCE = SimpleLoggerFactory.INSTANCE;
	final static SimpleLoggerFactory INSTANCE = (SimpleLoggerFactory) StaticLoggerBinder.getSingleton().getLoggerFactory();

	private SimpleLoggerAccess() {
	}

	/**
	 * Return an appropriate {@link SimpleLogger} instance by name, or
	 * {@code null} if none exists.
	 *
	 * @see ILoggerFactory#getLogger(String)
	 */
	public static SimpleLogger getLogger(String name) {
		// protect against concurrent access of the loggerMap
		synchronized (INSTANCE) {
			SimpleLogger slogger = (SimpleLogger) INSTANCE.loggerMap.get(name);
			return slogger;
		}
	}

	public static Collection<SimpleLogger> getAllLoggers() {
		synchronized (INSTANCE) {
			return new ArrayList<SimpleLogger>(INSTANCE.loggerMap.values());
		}
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
			return SimpleLogger.LOG_LEVEL_ALL;
		} else if ("trace".equalsIgnoreCase(lvl)) {
			return SimpleLogger.LOG_LEVEL_TRACE;
		} else if ("debug".equalsIgnoreCase(lvl)) {
			return SimpleLogger.LOG_LEVEL_DEBUG;
		} else if ("info".equalsIgnoreCase(lvl)) {
			return SimpleLogger.LOG_LEVEL_INFO;
		} else if ("warn".equalsIgnoreCase(lvl)) {
			return SimpleLogger.LOG_LEVEL_WARN;
		} else if ("error".equalsIgnoreCase(lvl)) {
			return SimpleLogger.LOG_LEVEL_ERROR;
//		} else if("fatal".equalsIgnoreCase(lvl)) {
//			return SimpleLogger.LOG_LEVEL_FATAL;
		} else if ("off".equalsIgnoreCase(lvl)) {
			return SimpleLogger.LOG_LEVEL_OFF;
		} else {
			throw new IllegalArgumentException(lvl);
		}
	}

	public static String getLevelName(SimpleLogger logger) throws IllegalArgumentException {
		return getLevelName(logger.currentLogLevel);
	}
	public static String getLevelName(int level) throws IllegalArgumentException {
	    switch(level) {
	      case SimpleLogger.LOG_LEVEL_ALL: return "ALL";
	      case SimpleLogger.LOG_LEVEL_TRACE: return "TRACE";
	      case SimpleLogger.LOG_LEVEL_DEBUG: return "DEBUG";
	      case SimpleLogger.LOG_LEVEL_INFO:  return "INFO";
	      case SimpleLogger.LOG_LEVEL_WARN:  return "WARN";
	      case SimpleLogger.LOG_LEVEL_ERROR: return "ERROR";
//	      case SimpleLogger.LOG_LEVEL_FATAL: return "FATAL";
	      case SimpleLogger.LOG_LEVEL_OFF: return "OFF";
	      default: throw new IllegalArgumentException(Integer.toString(level));
	    }
	}
}
