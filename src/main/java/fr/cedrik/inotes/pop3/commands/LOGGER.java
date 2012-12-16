/**
 *
 */
package fr.cedrik.inotes.pop3.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLogger;
import org.slf4j.impl.SimpleLoggerAccess;

import fr.cedrik.inotes.pop3.Context;
import fr.cedrik.inotes.pop3.POP3Command;
import fr.cedrik.inotes.pop3.ResponseStatus;
import fr.cedrik.inotes.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class LOGGER extends BasePOP3Command implements POP3Command {

	public LOGGER() {
	}

	@Override
	public boolean isValid(Context context) {
		return true;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		if (StringUtils.isEmpty(context.inputArgs)) {
			// list all available loggers
			Collection<SimpleLogger> allLoggers = SimpleLoggerAccess.getAllLoggers();
			List<String> result = new ArrayList<String>(allLoggers.size() + 1);
			result.add(ResponseStatus.POSITIVE.toString("loggers list follows (root logger: " + Logger.ROOT_LOGGER_NAME + ')'));
			for (SimpleLogger logger : allLoggers) {
				result.add(SimpleLoggerAccess.getLevelName(logger) + ' ' +  logger.getName());
			}
			return result.iterator();
		} else {
			// set logger level
			String requestedLoggerName;
			String requestedLevelName;
			SimpleLogger requestedLogger;
			StringTokenizer tokenizer = new StringTokenizer(context.inputArgs);
			if (tokenizer.countTokens() != 2) {
				return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString(getClass().getSimpleName()+" loggerName levelName"));
			}
			requestedLoggerName = tokenizer.nextToken();
			requestedLogger = SimpleLoggerAccess.getLogger(requestedLoggerName);
			if (requestedLogger == null) {
				return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("no such logger: " + requestedLoggerName));
			}
			requestedLevelName = tokenizer.nextToken();
			try {
				SimpleLoggerAccess.levelNameToInt(requestedLevelName);
			} catch (IllegalArgumentException noInput) {
				return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("bad level name: " + requestedLevelName));
			}
			SimpleLoggerAccess.changeLoggerLevel(requestedLogger, requestedLevelName);
			return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("logger " + requestedLogger.getName() + " changed to " + requestedLevelName));
		}
	}

}
