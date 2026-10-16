package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.ResolutionCardOfferContract;
import io.github.temporalrift.read.projection.domain.port.out.ResolutionCardOfferRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryResolutionCardOfferAdapterContractTest extends ResolutionCardOfferContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final ResolutionCardOfferRepository port = new InMemoryResolutionCardOfferAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected ResolutionCardOfferRepository port() {
        return port;
    }
}
