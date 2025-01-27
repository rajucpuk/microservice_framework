package uk.gov.justice.services.management.shuttering.process;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_COMPLETE;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_FAILED;

import uk.gov.justice.services.management.shuttering.api.ShutteringResult;

import java.util.List;

public class ShutteringResultsMapper {

    public List<ShutteringResult> getSuccessfulResults(final List<ShutteringResult> shutteringResults) {

        return shutteringResults.stream()
                .filter(shutteringResult -> shutteringResult.getCommandState() == COMMAND_COMPLETE)
                .collect(toList());
    }

    public List<ShutteringResult> getFailedResults(final List<ShutteringResult> shutteringResults) {

        return shutteringResults.stream()
                .filter(shutteringResult -> shutteringResult.getCommandState() == COMMAND_FAILED)
                .collect(toList());
    }

    public List<String> getShutteringExecutorNames(final List<ShutteringResult> shutteringResults) {
        return shutteringResults.stream()
                .map(ShutteringResult::getShutteringExecutorName)
                .collect(toList());
    }
}
