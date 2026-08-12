package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import io.github.temporalrift.read.projection.application.port.in.GetGameHistoryUseCase;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActiveEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CascadedEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.DealtHandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.EventOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryEra;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.HandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.Phase;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerInGame;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ResolvedOutcome;

/**
 * Maps projection query results to generated response DTOs.
 *
 * <p>Only the current-state slice's "core fields" are populated —
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

    static GameHistoryResponse toResponse(GetGameHistoryUseCase.Result result) {
        return new GameHistoryResponse(
                result.gameId(),
                result.eras().stream().map(ProjectionRestMapper::toHistoryEra).toList());
    }

    private static GameHistoryEra toHistoryEra(GetGameHistoryUseCase.EraResult era) {
        var response = new GameHistoryEra(
                era.eraNumber(),
                era.outcomes().stream()
                        .map(outcome -> new ResolvedOutcome(
                                outcome.eventId(),
                                outcome.title(),
                                outcome.winningOutcomeId(),
                                outcome.winningOutcomeDescription()))
                        .toList(),
                era.paradoxesCascaded(),
                era.myHand().stream().map(ProjectionRestMapper::toDealtHandCard).toList());
        response.setCascadedEvents(
                era.cascadedEvents().isEmpty()
                        ? null
                        : era.cascadedEvents().stream()
                                .map(event -> new CascadedEvent(event.eventId(), event.title()))
                                .toList());
        return response;
    }

    private static DealtHandCard toDealtHandCard(io.github.temporalrift.read.projection.domain.model.DealtCard domain) {
        return new DealtHandCard(domain.cardInstanceId(), domain.cardType());
    }

    private static HandCard toHandCard(io.github.temporalrift.read.projection.domain.model.HandCard domain) {
        return new HandCard(domain.cardInstanceId(), domain.cardType());
    }

    private static ActiveEvent toActiveEvent(
            io.github.temporalrift.read.projection.domain.model.GameActiveEvent domain) {
        var outcomes = domain.outcomes().stream()
                .map(o -> new EventOutcome(o.outcomeId(), o.description()))
                .toList();
        return new ActiveEvent(
                domain.eventId(),
                domain.title(),
                ActiveEvent.CarryOverStateEnum.valueOf(domain.carryOverState().name()),
                outcomes);
    }

    private static PlayerInGame toPlayerInGame(io.github.temporalrift.read.projection.domain.model.GamePlayer domain) {
        var playerInGame = new PlayerInGame(domain.playerId(), domain.score(), domain.isConnected());
        playerInGame.setFaction(domain.faction());
        return playerInGame;
    }
}
