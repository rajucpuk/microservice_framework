package uk.gov.justice.services.management.shuttering.executors;

import static javax.transaction.Transactional.TxType.REQUIRED;

import uk.gov.justice.services.common.polling.MultiIteratingPoller;
import uk.gov.justice.services.common.polling.MultiIteratingPollerFactory;
import uk.gov.justice.services.messaging.jms.EnvelopeSenderSelector;
import uk.gov.justice.services.shuttering.domain.StoredCommand;
import uk.gov.justice.services.shuttering.persistence.StoredCommandRepository;

import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Stateless
public class CommandApiShutteringBean {

    private static final int POLLER_RETRY_COUNT = 2;
    private static final long POLLER_DELAY_INTERVAL_MILLIS = 100;

    private static final int NUMBER_OF_POLLING_ITERATIONS = 2;
    private static final long WAIT_TIME_BETWEEN_ITERATIONS_MILLIS = 100;

    @Inject
    private EnvelopeSenderSelector envelopeSenderSelector;

    @Inject
    private StoredCommandRepository storedCommandRepository;

    @Inject
    private StoredCommandSender storedCommandSender;

    @Inject
    private MultiIteratingPollerFactory multiIteratingPollerFactory;

    public void shutter() {
        envelopeSenderSelector.setShuttered(true);
    }

    @Transactional(REQUIRED)
    public void unshutter() {

        getCommandsAndSend();

        envelopeSenderSelector.setShuttered(false);

        final MultiIteratingPoller multiIteratingPoller = multiIteratingPollerFactory.create(
                POLLER_RETRY_COUNT,
                POLLER_DELAY_INTERVAL_MILLIS,
                NUMBER_OF_POLLING_ITERATIONS,
                WAIT_TIME_BETWEEN_ITERATIONS_MILLIS);
        
        multiIteratingPoller.pollUntilTrue(this::getCommandsAndSend);
    }

    private boolean getCommandsAndSend() {
        try (final Stream<StoredCommand> storedCommandStream = storedCommandRepository.streamStoredCommands()) {
            storedCommandStream.forEach(storedCommandSender::sendAndDelete);
        }

        return true;
    }
}
