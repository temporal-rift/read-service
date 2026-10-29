package io.github.temporalrift.read.simulation.domain.execution;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * The immutable inputs of one isolated game, as far as read-service keeps them: enough to tell an identical repeat
 * from a different context. The rules are held by their digest, which {@link #of} checks against the rules themselves;
 * times are kept at microsecond precision.
 */
public record ExecutionContext(
        UUID caseKey,
        Seed seed,
        EntropyVersion entropyVersion,
        String manifestDigest,
        String rulesDigest,
        Instant logicalTime,
        List<SimulationSeat> seats) {

    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");
    private static final int MIN_SEATS = 3;
    private static final int MAX_SEATS = 5;

    public ExecutionContext {
        Objects.requireNonNull(caseKey, "caseKey");
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(entropyVersion, "entropyVersion");
        logicalTime = Objects.requireNonNull(logicalTime, "logicalTime").truncatedTo(ChronoUnit.MICROS);
        if (manifestDigest == null || !SHA256_HEX.matcher(manifestDigest).matches()) {
            throw new InvalidExecutionContextException("manifestDigest must be a lowercase SHA-256 hex digest");
        }
        if (rulesDigest == null || !SHA256_HEX.matcher(rulesDigest).matches()) {
            throw new InvalidExecutionContextException("rulesDigest must be a lowercase SHA-256 hex digest");
        }
        seats = List.copyOf(Objects.requireNonNull(seats, "seats"));
        requireValidSeats(seats);
    }

    /** A context received from the operator, whose {@code rulesDigest} must be the digest of its {@code rules}. */
    public static ExecutionContext of(
            UUID caseKey,
            Seed seed,
            EntropyVersion entropyVersion,
            String manifestDigest,
            EffectiveRules rules,
            String rulesDigest,
            Instant logicalTime,
            List<SimulationSeat> seats) {
        var context =
                new ExecutionContext(caseKey, seed, entropyVersion, manifestDigest, rulesDigest, logicalTime, seats);
        if (!rulesDigest.equals(Objects.requireNonNull(rules, "rules").digest())) {
            throw new InvalidExecutionContextException("rulesDigest does not match the digest of the rules");
        }
        return context;
    }

    private static void requireValidSeats(List<SimulationSeat> seats) {
        if (seats.size() < MIN_SEATS || seats.size() > MAX_SEATS) {
            throw new InvalidExecutionContextException("seats must hold between 3 and 5 entries");
        }
        if (!IntStream.range(0, seats.size()).allMatch(index -> seats.get(index).seatIndex() == index)) {
            throw new InvalidExecutionContextException("seat indices must be contiguous from 0 in order");
        }
        if (seats.stream().anyMatch(seat -> seat.playerId() == null || seat.faction() == null)) {
            throw new InvalidExecutionContextException("every seat needs a player and a faction");
        }
        var players = new HashSet<UUID>();
        var factions = new HashSet<String>();
        for (var seat : seats) {
            if (!players.add(seat.playerId())) {
                throw new InvalidExecutionContextException("a player may occupy only one seat");
            }
            if (!factions.add(seat.faction())) {
                throw new InvalidExecutionContextException("a faction may be assigned to only one seat");
            }
        }
    }
}
