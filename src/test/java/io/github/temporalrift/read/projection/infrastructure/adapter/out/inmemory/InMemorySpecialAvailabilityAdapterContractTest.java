package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.SpecialAvailabilityContract;
import io.github.temporalrift.read.projection.domain.port.out.SpecialAvailabilityRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemorySpecialAvailabilityAdapterContractTest extends SpecialAvailabilityContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final SpecialAvailabilityRepository port = new InMemorySpecialAvailabilityAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected SpecialAvailabilityRepository port() {
        return port;
    }
}
