package uk.gov.justice.services.management.shuttering.observers.unshuttering;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.jmx.api.state.ApplicationManagementState.UNSHUTTERED;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.getValueOfField;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.command.ApplicationManagementStateRegistry;
import uk.gov.justice.services.management.shuttering.events.UnshutteringCompleteEvent;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.enterprise.event.Event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class UnshutteringRegistryTest {

    @Mock
    private Event<UnshutteringCompleteEvent> unshutteringCompleteEventFirer;

    @Mock
    private ApplicationManagementStateRegistry applicationManagementStateRegistry;

    @Mock
    private UtcClock clock;

    @Mock
    private Logger logger;

    @InjectMocks
    private UnshutteringRegistry unshutteringRegistry;

    @Test
    public void shouldFireUnshutteringCompleteOnceAllUnshutterablesAreMarkedAsComplete() throws Exception {

        final String commandName = "CATCHUP";
        final SystemCommand systemCommand = mock(SystemCommand.class);
        final ZonedDateTime now = new UtcClock().now();

        when(clock.now()).thenReturn(now);
        when(systemCommand.getName()).thenReturn(commandName);

        clearShutteringRegistry();

        unshutteringRegistry.registerAsUnshutterable(Unshutterable_1.class);
        verify(logger).info("Registering Unshutterable_1 as unshuttering executor");

        unshutteringRegistry.registerAsUnshutterable(Unshutterable_2.class);
        verify(logger).info("Registering Unshutterable_2 as unshuttering executor");

        unshutteringRegistry.registerAsUnshutterable(Unshutterable_3.class);
        verify(logger).info("Registering Unshutterable_3 as unshuttering executor");

        unshutteringRegistry.unshutteringStarted();

        unshutteringRegistry.markUnshutteringCompleteFor(Unshutterable_2.class, systemCommand);
        verify(logger).info("Marking unshuttering complete for Unshutterable_2");

        unshutteringRegistry.markUnshutteringCompleteFor(Unshutterable_1.class, systemCommand);
        verify(logger).info("Marking unshuttering complete for Unshutterable_1");

        unshutteringRegistry.markUnshutteringCompleteFor(Unshutterable_3.class, systemCommand);
        verify(logger).info("Marking unshuttering complete for Unshutterable_3");

        verify(logger).info("All unshuttering complete: [Unshutterable_1, Unshutterable_2, Unshutterable_3]");
        verify(applicationManagementStateRegistry).setApplicationManagementState(UNSHUTTERED);
        verify(unshutteringCompleteEventFirer, times(1)).fire(new UnshutteringCompleteEvent(systemCommand, now));
    }

    @Test
    public void shouldNotFireUnshutteringCompleteIfNotAllUnshutterablesAreMarkedAsComplete() throws Exception {

        final SystemCommand systemCommand = mock(SystemCommand.class);

        clearShutteringRegistry();

        unshutteringRegistry.registerAsUnshutterable(Unshutterable_1.class);
        unshutteringRegistry.registerAsUnshutterable(Unshutterable_2.class);
        unshutteringRegistry.registerAsUnshutterable(Unshutterable_3.class);

        unshutteringRegistry.unshutteringStarted();

        unshutteringRegistry.markUnshutteringCompleteFor(Unshutterable_2.class, systemCommand);
        unshutteringRegistry.markUnshutteringCompleteFor(Unshutterable_3.class, systemCommand);

        verifyZeroInteractions(unshutteringCompleteEventFirer);
    }

    private void clearShutteringRegistry() throws Exception {
        getValueOfField(unshutteringRegistry, "unshutteringStateMap", Map.class).clear();
    }
}

class Unshutterable_1 {}

class Unshutterable_2 {}

class Unshutterable_3 {}
