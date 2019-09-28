package uk.gov.justice.services.jmx.runner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.getValueOfField;

import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.command.TestCommand;

import java.util.UUID;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AsynchronousCommandRunnerTest {

    @Mock
    private ManagedExecutorService managedExecutorService;

    @Mock
    private SystemCommandRunner systemCommandRunner;

    @InjectMocks
    private AsynchronousCommandRunner asynchronousCommandRunner;

    @Captor
    private ArgumentCaptor<RunSystemCommandTask> runSystemCommandTaskCaptor;

    @Test
    public void shouldRunTheSystemCommandAsynchronously() throws Exception {

        final SystemCommand systemCommand = new TestCommand();

        final UUID commandId = asynchronousCommandRunner.run(systemCommand);

        verify(managedExecutorService).submit(runSystemCommandTaskCaptor.capture());

        final RunSystemCommandTask runSystemCommandTask = runSystemCommandTaskCaptor.getValue();

        assertThat(getValueOfField(runSystemCommandTask, "systemCommandRunner", SystemCommandRunner.class), is(systemCommandRunner));
        assertThat(getValueOfField(runSystemCommandTask, "systemCommand", SystemCommand.class), is(systemCommand));
        assertThat(getValueOfField(runSystemCommandTask, "commandId", UUID.class), is(commandId));
    }

    @Test
    public void shouldCheckThatCommandIsSupported() throws Exception {

        final boolean supported = true;
        final SystemCommand systemCommand = new TestCommand();

        when(systemCommandRunner.isSupported(systemCommand)).thenReturn(supported);

        assertThat(asynchronousCommandRunner.isSupported(systemCommand), is(supported));
    }
}