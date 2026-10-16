package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.ExposeFactContract;
import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;

@ReadServiceIntegrationTest
class JpaExposeFactAdapterContractIT extends ExposeFactContract {

    @Autowired
    ExposeFactRepository port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected ExposeFactRepository port() {
        return port;
    }
}
