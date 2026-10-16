package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.GameChainContract;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;

@ReadServiceIntegrationTest
class JpaGameChainAdapterContractIT extends GameChainContract {

    @Autowired
    GameChainRepository port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected GameChainRepository port() {
        return port;
    }
}
