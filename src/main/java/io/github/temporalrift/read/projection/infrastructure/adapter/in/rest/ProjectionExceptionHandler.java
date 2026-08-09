package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.temporalrift.read.projection.domain.model.GameHistoryNotFoundException;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;

@RestControllerAdvice
class ProjectionExceptionHandler {

    @ExceptionHandler(PlayerNotInGameException.class)
    ProblemDetail handlePlayerNotInGame(PlayerNotInGameException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(GameHistoryNotFoundException.class)
    ProblemDetail handleGameHistoryNotFound(GameHistoryNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
