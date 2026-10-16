package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.PlayerSubmissionContract;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryPlayerSubmissionAdapterContractTest extends PlayerSubmissionContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final PlayerSubmissionRepository port = new InMemoryPlayerSubmissionAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected PlayerSubmissionRepository port() {
        return port;
    }
}
