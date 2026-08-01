package io.github.temporalrift.read.shared;

import java.io.Serializable;
import java.util.UUID;

public record PlayerPrincipal(UUID playerId) implements Serializable {}
