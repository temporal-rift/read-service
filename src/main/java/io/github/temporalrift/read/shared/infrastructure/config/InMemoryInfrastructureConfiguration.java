package io.github.temporalrift.read.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

/** The store and transaction manager that stand in for PostgreSQL and JPA under the {@code in-memory} profile. */
@Configuration
@Profile("in-memory")
class InMemoryInfrastructureConfiguration {

    @Bean
    InMemoryDatabase inMemoryDatabase() {
        return new InMemoryDatabase();
    }

    @Bean
    PlatformTransactionManager transactionManager(InMemoryDatabase database) {
        return new InMemoryTransactionManager(database);
    }
}
