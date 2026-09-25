package io.github.temporalrift.read.projection.application;

import org.springframework.stereotype.Component;

import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerNameRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PublicBandRepository;
import io.github.temporalrift.read.projection.domain.port.out.PublicDeclarationRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.TerminalResultRepository;

/**
 * The driven-port bundle shared by the participant query and the event applier. Both read and
 * write the same projections, so both declare the same dependencies — this
 * record keeps that list in one place instead of duplicating it across constructors.
 */
@Component
public record ProjectionRepositories(
        GameProjectionRepository gameProjections,
        GamePlayerRepository gamePlayers,
        GameActiveEventRepository gameActiveEvents,
        PlayerGameStateRepository playerGameStates,
        RevealedProbabilityIntelRepository revealedProbabilityIntel,
        RevealedInfluenceIntelRepository revealedInfluenceIntel,
        RevealedHandCardIntelRepository revealedHandCardIntel,
        GameChainRepository gameChains,
        PublicBandRepository publicBands,
        PublicDeclarationRepository publicDeclarations,
        ExposeFactRepository exposeFacts,
        PlayerSubmissionRepository playerSubmissions,
        TerminalResultRepository terminalResults,
        PlayerNameRepository playerNames) {}
