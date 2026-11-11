package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.SpecialAvailabilityContract;
import io.github.temporalrift.read.projection.domain.port.out.SpecialAvailabilityRepository;

@ReadServiceIntegrationTest
class JpaSpecialAvailabilityAdapterContractIT extends SpecialAvailabilityContract {

    @Autowired
    SpecialAvailabilityRepository port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected SpecialAvailabilityRepository port() {
        return port;
    }
}
