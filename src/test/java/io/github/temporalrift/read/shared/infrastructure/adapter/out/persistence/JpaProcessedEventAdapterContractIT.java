package io.github.temporalrift.read.shared.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.ProcessedEventContract;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@ReadServiceIntegrationTest
class JpaProcessedEventAdapterContractIT extends ProcessedEventContract {

    @Autowired
    ProcessedEventPort port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected ProcessedEventPort port() {
        return port;
    }
}
