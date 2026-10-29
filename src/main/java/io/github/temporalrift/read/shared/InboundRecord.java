package io.github.temporalrift.read.shared;

import java.util.UUID;

/**
 * Where an inbound record was read from and which game it belongs to. The source position is absent when the
 * transport has none, and {@code gameId} when the envelope carries no valid one.
 */
public record InboundRecord(String topic, Integer partition, Long offset, UUID gameId) {}
