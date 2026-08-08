package io.github.temporalrift.read.notification.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public final class WebSocketBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/ws/games/")) {
            var token = request.getParameter("token");
            if (token != null && !token.isBlank()) {
                return token;
            }
        }
        return delegate.resolve(request);
    }
}
