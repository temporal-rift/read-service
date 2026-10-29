package io.github.temporalrift.read.simulation.domain.execution;

public class ExecutionContextConflictException extends RuntimeException {

    public ExecutionContextConflictException(String message) {
        super(message);
    }
}
