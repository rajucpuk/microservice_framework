package uk.gov.justice.services.management.shuttering.process;

import static java.lang.String.format;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_COMPLETE;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_FAILED;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jmx.api.command.ApplicationShutteringCommand;
import uk.gov.justice.services.jmx.state.events.SystemCommandStateChangedEvent;
import uk.gov.justice.services.management.shuttering.api.ShutteringResult;

import java.util.List;
import java.util.UUID;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

public class ShutteringPostProcess {

    @Inject
    private ShutteringResultsMapper shutteringResultsMapper;

    @Inject
    private Event<SystemCommandStateChangedEvent> systemCommandStateChangedEventFirer;

    @Inject
    private UtcClock clock;

    @Inject
    private Logger logger;

    public void completeShutteringSuccessfully(final List<ShutteringResult> successfulResults, final UUID commandId, final ApplicationShutteringCommand applicationShutteringCommand) {

        final String applicationShutteringCommandName = applicationShutteringCommand.getName();

        final List<String> shutteringExecutorNames = shutteringResultsMapper.getShutteringExecutorNames(successfulResults);

        final String message = format(
                "%s completed successfully for %s",
                applicationShutteringCommandName,
                shutteringExecutorNames);

        systemCommandStateChangedEventFirer.fire(new SystemCommandStateChangedEvent(
                commandId,
                applicationShutteringCommand,
                COMMAND_COMPLETE,
                clock.now(),
                message
        ));
    }

    public void completeShutteringWithFailures(final List<ShutteringResult> failureResults, final UUID commandId, final ApplicationShutteringCommand applicationShutteringCommand) {

        final String applicationShutteringCommandName = applicationShutteringCommand.getName();

        logger.error(format("%s failed with the following %d errors:", applicationShutteringCommandName, failureResults.size()));

        failureResults.forEach(shutteringResult -> logger.error(format("%s Error: %s", applicationShutteringCommandName, shutteringResult.getMessage())));

        final List<String> shutteringExecutorNames = shutteringResultsMapper
                .getShutteringExecutorNames(failureResults);

        final String message = format(
                "%s failed. The following ShutteringExecutors failed: %s",
                applicationShutteringCommandName,
                shutteringExecutorNames);

        systemCommandStateChangedEventFirer.fire(new SystemCommandStateChangedEvent(
                commandId,
                applicationShutteringCommand,
                COMMAND_FAILED,
                clock.now(),
                message
        ));
    }
}
