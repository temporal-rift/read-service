package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.contract.ResolutionCardOfferContract;
import io.github.temporalrift.read.projection.domain.port.out.ResolutionCardOfferRepository;

@ReadServiceIntegrationTest
class JpaResolutionCardOfferAdapterContractIT extends ResolutionCardOfferContract {

    @Autowired
    ResolutionCardOfferRepository port;

    @Autowired
    TransactionTemplate transactions;

    @Override
    protected TransactionOperations transactions() {
        return transactions;
    }

    @Override
    protected ResolutionCardOfferRepository port() {
        return port;
    }
}
