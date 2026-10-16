package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.RevealedInfluenceIntelContract;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryRevealedInfluenceIntelAdapterContractTest extends RevealedInfluenceIntelContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final RevealedInfluenceIntelRepository port = new InMemoryRevealedInfluenceIntelAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected RevealedInfluenceIntelRepository port() {
        return port;
    }
}
