package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.PlayerSubmissionContract;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;

@ReadServiceIntegrationTest
class JpaPlayerSubmissionAdapterContractIT extends PlayerSubmissionContract {

    @Autowired
    PlayerSubmissionRepository port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected PlayerSubmissionRepository port() {
        return port;
    }
}
