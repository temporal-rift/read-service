package io.github.temporalrift.read.simulation.domain.execution;

public class ExecutionStateNotCleanException extends RuntimeException {

    public ExecutionStateNotCleanException(String message) {
        super(message);
    }
}
