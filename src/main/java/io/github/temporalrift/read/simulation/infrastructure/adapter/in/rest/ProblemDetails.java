package io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Creates the control contract's problem details, which carry a stable {@code code}. */
public final class ProblemDetails {

    private ProblemDetails() {}

    public static ProblemDetail of(HttpStatusCode status, String detail, String code) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
