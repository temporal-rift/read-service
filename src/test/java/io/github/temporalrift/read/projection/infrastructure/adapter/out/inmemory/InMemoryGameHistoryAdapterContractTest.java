package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.GameHistoryContract;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryGameHistoryAdapterContractTest extends GameHistoryContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final GameHistoryRepository port = new InMemoryGameHistoryAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected GameHistoryRepository port() {
        return port;
    }
}
