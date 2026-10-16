package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.GameChainContract;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryGameChainAdapterContractTest extends GameChainContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final GameChainRepository port = new InMemoryGameChainAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected GameChainRepository port() {
        return port;
    }
}
