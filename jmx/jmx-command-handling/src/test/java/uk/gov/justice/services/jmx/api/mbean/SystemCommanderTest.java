package uk.gov.justice.services.jmx.api.mbean;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.jmx.api.CommandNotFoundException;
import uk.gov.justice.services.jmx.api.UnrunnableSystemCommandException;
import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.api.domain.SystemCommandStatus;
import uk.gov.justice.services.jmx.command.SystemCommandScanner;
import uk.gov.justice.services.jmx.command.TestCommand;
import uk.gov.justice.services.jmx.runner.AsynchronousCommandRunner;
import uk.gov.justice.services.jmx.state.observers.SystemCommandStateBean;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class SystemCommanderTest {

    @Mock
    private Logger logger;

    @Mock
    private AsynchronousCommandRunner asynchronousCommandRunner;

    @Mock
    private SystemCommandScanner systemCommandScanner;

    @Mock
    private SystemCommandStateBean systemCommandStateBean;

    @InjectMocks
    private SystemCommander systemCommander;

    @Test
    public void shouldRunTheSystemCommandIfSupported() throws Exception {

        final UUID commandId = randomUUID();
        final TestCommand testCommand = new TestCommand();

        when(asynchronousCommandRunner.commandNotSupported(testCommand)).thenReturn(false);
        when(asynchronousCommandRunner.run(testCommand)).thenReturn(commandId);

        assertThat(systemCommander.call(testCommand), is(commandId));

        final InOrder inOrder = inOrder(logger, asynchronousCommandRunner);

        inOrder.verify(logger).info("Received System Command 'TEST_COMMAND'");
        inOrder.verify(asynchronousCommandRunner).run(testCommand);
    }

    @Test
    public void shouldFailIfSystemCommandNotSupported() throws Exception {

        final TestCommand testCommand = new TestCommand();

        when(asynchronousCommandRunner.commandNotSupported(testCommand)).thenReturn(true);

        try {
            systemCommander.call(testCommand);
            fail();
        } catch (final UnrunnableSystemCommandException expected) {
            assertThat(expected.getMessage(), is("The system command 'TEST_COMMAND' is not supported on this context."));
        }
    }

    @Test
    public void shouldFailIfPreviousSystemCommandIsInProgress() throws Exception {

        final TestCommand testCommand = new TestCommand();

        when(asynchronousCommandRunner.commandNotSupported(testCommand)).thenReturn(false);
        when(systemCommandStateBean.commandInProgress(testCommand)).thenReturn(true);

        try {
            systemCommander.call(testCommand);
            fail();
        } catch (final UnrunnableSystemCommandException expected) {
            assertThat(expected.getMessage(), is("Cannot run system command 'TEST_COMMAND'. A previous call to that command is still in progress."));
        }
    }

    @Test
    public void shouldListAllSystemCommands() throws Exception {

        final SystemCommand systemCommand_1 = mock(SystemCommand.class);
        final SystemCommand systemCommand_2 = mock(SystemCommand.class);
        final SystemCommand systemCommand_3 = mock(SystemCommand.class);

        when(systemCommandScanner.findCommands()).thenReturn(asList(
                systemCommand_1,
                systemCommand_2,
                systemCommand_3));

        final List<SystemCommand> systemCommands = systemCommander.listCommands();

        assertThat(systemCommands.size(), is(3));
        assertThat(systemCommands, hasItem(systemCommand_1));
        assertThat(systemCommands, hasItem(systemCommand_2));
        assertThat(systemCommands, hasItem(systemCommand_3));
    }

    @Test
    public void shouldGetSystemCommandStatus() throws Exception {

        final UUID commandId = randomUUID();

        final SystemCommandStatus systemCommandStatus = mock(SystemCommandStatus.class);
        when(systemCommandStateBean.getCommandStatus(commandId)).thenReturn(of(systemCommandStatus));

        assertThat(systemCommander.getCommandStatus(commandId), is(systemCommandStatus));
    }

    @Test
    public void shouldThrowExceptionIfSystemCommandNotFound() throws Exception {

        final UUID commandId = fromString("08fe90e9-c35b-4850-9af2-e5e743f6736e");

        when(systemCommandStateBean.getCommandStatus(commandId)).thenReturn(empty());

        try {
            systemCommander.getCommandStatus(commandId);
            fail();
        } catch (CommandNotFoundException expected) {
            assertThat(expected.getMessage(), is("No SystemCommand found with id 08fe90e9-c35b-4850-9af2-e5e743f6736e"));
        }
    }
}
