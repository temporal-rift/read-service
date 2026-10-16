package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.RevealedInfluenceIntelContract;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;

@ReadServiceIntegrationTest
class JpaRevealedInfluenceIntelAdapterContractIT extends RevealedInfluenceIntelContract {

    @Autowired
    RevealedInfluenceIntelRepository port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected RevealedInfluenceIntelRepository port() {
        return port;
    }
}
