package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.ForesightPreviewContract;
import io.github.temporalrift.read.projection.domain.port.out.ForesightPreviewRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryForesightPreviewAdapterContractTest extends ForesightPreviewContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final ForesightPreviewRepository port = new InMemoryForesightPreviewAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected ForesightPreviewRepository port() {
        return port;
    }
}
