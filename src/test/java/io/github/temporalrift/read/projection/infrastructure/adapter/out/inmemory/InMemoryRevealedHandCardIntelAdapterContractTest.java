package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.RevealedHandCardIntelContract;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryRevealedHandCardIntelAdapterContractTest extends RevealedHandCardIntelContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final RevealedHandCardIntelRepository port = new InMemoryRevealedHandCardIntelAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected RevealedHandCardIntelRepository port() {
        return port;
    }
}
