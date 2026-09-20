package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TerminalResultJpaRepository extends JpaRepository<TerminalResultEntity, UUID> {}
