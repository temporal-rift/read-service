package io.github.temporalrift.read.simulation.domain.execution;

public class InvalidClockAdvanceException extends RuntimeException {

    public InvalidClockAdvanceException(String message) {
        super(message);
    }
}
