package uk.gov.justice.services.jmx.runner;

import static java.lang.String.format;
import static javax.transaction.Transactional.TxType.NEVER;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_IN_PROGRESS;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_RECEIVED;

import uk.gov.justice.services.framework.utilities.exceptions.StackTraceProvider;
import uk.gov.justice.services.jmx.api.SystemCommandInvocationFailedException;
import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.api.domain.CommandState;
import uk.gov.justice.services.jmx.api.domain.SystemCommandStatus;
import uk.gov.justice.services.jmx.command.SystemCommandStore;
import uk.gov.justice.services.jmx.state.persistence.SystemCommandStatusRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;

public class SystemCommandRunner {

    @Inject
    private SystemCommandStore systemCommandStore;

    @Inject
    private StackTraceProvider stackTraceProvider;

    @Inject
    private Logger logger;

    @Transactional(NEVER)
    public boolean isSupported(final SystemCommand systemCommand) {
        return systemCommandStore.isSupported(systemCommand);
    }

    @Transactional(NEVER)
    public void run(final SystemCommand systemCommand, final UUID commandId) {

        try {
            systemCommandStore.findCommandProxy(systemCommand).invokeCommand(systemCommand, commandId);
        } catch (final Throwable e) {
            final String message = format("Failed to run System Command '%s'", systemCommand.getName());
            logger.error(message, e);

            throw new SystemCommandInvocationFailedException(
                    message + ". Caused by " + e.getClass().getName() + ": " + e.getMessage(),
                    stackTraceProvider.getStackTrace(e));
        }
    }
}
