package io.github.temporalrift.read.shared.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.ProcessedEventContract;
import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryProcessedEventAdapterContractTest extends ProcessedEventContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final ProcessedEventPort port = new InMemoryProcessedEventAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected ProcessedEventPort port() {
        return port;
    }
}
