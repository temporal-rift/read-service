package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.contract.DeclarationWindowContract;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationWindowRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class InMemoryDeclarationWindowAdapterContractTest extends DeclarationWindowContract {

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final TransactionTemplate transactions = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final DeclarationWindowRepository port = new InMemoryDeclarationWindowAdapter(database);

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected DeclarationWindowRepository port() {
        return port;
    }
}
