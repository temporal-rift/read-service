package io.github.temporalrift.read;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Consumer;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.KafkaOperations;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;

/** Starts the application the way a simulation host does: a child of the host context, no database, no broker. */
class InMemoryApplicationTest {

    @Test
    void startsUnderAParentContextAndServesTheProjectionApiWithoutDatabaseOrBroker() throws Exception {
        try (var host = new AnnotationConfigApplicationContext();
                var application = start(host)) {
            assertThat(application.getBeanNamesForType(DataSource.class)).isEmpty();
            assertThat(application.getBeanNamesForType(KafkaOperations.class)).isEmpty();
            assertThat(application.getBean("simulationEventDelivery")).isInstanceOf(Consumer.class);
            assertThat(application.getBeanNamesForType(GetPlayerGameStateUseCase.class))
                    .isNotEmpty();

            var health = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(
                                            URI.create("http://localhost:" + port(application) + "/actuator/health"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoInstancesInOneJvmShareNoState() {
        try (var firstHost = new AnnotationConfigApplicationContext();
                var secondHost = new AnnotationConfigApplicationContext();
                var first = start(firstHost);
                var second = start(secondHost)) {
            var delivery = (Consumer<Map<String, Object>>) first.getBean("simulationEventDelivery");
            var unknownTopic = Map.<String, Object>of("topic", "other.topic", "payload", new byte[0]);

            delivery.accept(unknownTopic);

            assertThat(first.getBean("inMemoryDatabase")).isNotSameAs(second.getBean("inMemoryDatabase"));
            assertThat(first.getBean("simulationEventDelivery")).isNotSameAs(second.getBean("simulationEventDelivery"));
        }
    }

    static ConfigurableApplicationContext start(AnnotationConfigApplicationContext host) {
        host.refresh();
        return new SpringApplicationBuilder(ReadServiceApplication.class)
                .parent(host)
                .profiles("in-memory")
                .run(
                        "--server.port=0",
                        "--game.rules.max-eras=5",
                        "--spring.security.oauth2.resourceserver.jwt.issuer-uri=https://host-issuer.invalid",
                        "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://host-issuer.invalid/jwks");
    }

    private static int port(ConfigurableApplicationContext application) {
        return ((WebServerApplicationContext) application).getWebServer().getPort();
    }
}
