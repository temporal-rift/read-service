package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.RevealedProbabilityIntelContract;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryRevealedProbabilityIntelAdapterContractTest extends RevealedProbabilityIntelContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final RevealedProbabilityIntelRepository port = new InMemoryRevealedProbabilityIntelAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected RevealedProbabilityIntelRepository port() {
        return port;
    }
}
