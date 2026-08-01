package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.model.ActiveEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.model.EventOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.model.HandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.model.Phase;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.model.PlayerGameStateResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.model.PlayerInGame;

/**
 * Maps the query result to the generated response DTO. Only this slice's "core fields" are populated —
 * {@code mySpecialActions}, {@code myJammedUntilRound}, {@code lastRoundSummary} stay unset (deferred to a
 * later slice, design.md Non-Goals).
 */
final class ProjectionRestMapper {

    private ProjectionRestMapper() {}

    static PlayerGameStateResponse toResponse(GetPlayerGameStateUseCase.Result result) {
        var response = new PlayerGameStateResponse(
                result.gameId(),
                result.eraNumber(),
                Phase.valueOf(result.phase().name()),
                result.myHand().stream().map(ProjectionRestMapper::toHandCard).toList(),
                result.myScore(),
                result.activeEvents().stream()
                        .map(ProjectionRestMapper::toActiveEvent)
                        .toList(),
                result.players().stream()
                        .map(ProjectionRestMapper::toPlayerInGame)
                        .toList());
        response.setMyFaction(result.myFaction());
        return response;
    }

    private static HandCard toHandCard(io.github.temporalrift.read.projection.domain.model.HandCard domain) {
        return new HandCard(domain.cardInstanceId(), domain.cardType());
    }

    private static ActiveEvent toActiveEvent(
            io.github.temporalrift.read.projection.domain.model.GameActiveEvent domain) {
        var outcomes = domain.outcomes().stream()
                .map(o -> new EventOutcome(o.outcomeId(), o.description()))
                .toList();
        return new ActiveEvent(domain.eventId(), domain.title(), domain.isCascaded(), outcomes);
    }

    private static PlayerInGame toPlayerInGame(io.github.temporalrift.read.projection.domain.model.GamePlayer domain) {
        var playerInGame = new PlayerInGame(domain.playerId(), domain.score(), domain.isConnected());
        playerInGame.setFaction(domain.faction());
        return playerInGame;
    }
}
