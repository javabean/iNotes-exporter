/**
 *
 */
package fr.cedrik.email.pop3.commands;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import fr.cedrik.email.MessagesMetaData;
import fr.cedrik.email.pop3.Context;
import fr.cedrik.email.pop3.POP3Command;
import fr.cedrik.email.pop3.ResponseStatus;
import fr.cedrik.email.pop3.State;
import fr.cedrik.util.IteratorChain;

/**
 * @author C&eacute;drik LIME
 */
public class PASS extends BasePOP3Command implements POP3Command {

	public PASS() {
	}

	@Override
	public boolean isValid(Context context) {
		return context.state == State.AUTHORIZATION && StringUtils.isNotBlank(context.userName);
	}

	@Override
	public State nextState(Context context) {
		return (StringUtils.isBlank(context.userName) || StringUtils.isBlank(context.userPassword)) ? State.AUTHORIZATION : State.TRANSACTION;
	}

	@Override
	public Iterator<String> call(Context context) throws IOException {
		if (StringUtils.isBlank(context.inputArgs)) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("[AUTH] empty passwords are not allowed"));
		}
		if (StringUtils.isBlank(context.userName)) {
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("[SYS/TEMP] must call USER first"));
		}
		if (context.isLocked()) {
			logger.warn("An attempt was made to authenticate the locked user \"{}\"", context.userName);
			context.userName = null;
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("[AUTH] user is locked"));
		}
		context.userPassword = context.inputArgs;
		if (context.remoteSession.login(context.userName, context.userPassword)) {
			context.registerAuthSuccess();
			context.state = nextState(context);
			String quotaMessage = "";
			{
				MessagesMetaData<?> messages = context.remoteSession.getMessagesMetaData(0);
				if (messages.ignorequota == 0 && messages.sizelimit > 0) {
					if (messages.dbsize >= messages.sizelimit || messages.currentusage >= messages.sizelimit) {
						quotaMessage = ". WARNING WARNING: you have exceeded your quota! Run QUOTA command for more information.";
					} else if (messages.dbsize > messages.warning || messages.currentusage > messages.warning) {
						quotaMessage = ". WARNING: you are nearing your quota. Run QUOTA command for more information.";
					}
				}
			}
			return new IteratorChain<String>(ResponseStatus.POSITIVE.toString("welcome, " + context.userName + quotaMessage));
		} else {
			context.registerAuthFailure();
			context.userPassword = null;
			return new IteratorChain<String>(ResponseStatus.NEGATIVE.toString("[AUTH] invalid user or password"));
		}
	}

}
