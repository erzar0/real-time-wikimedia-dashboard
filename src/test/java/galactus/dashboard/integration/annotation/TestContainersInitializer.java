package galactus.dashboard.integration.annotation;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;


/**
 * Initializes TestContainers for integration testing.
 * <p>
 * This class configures and starts the required TestContainers, such as PostgreSQL and Kafka, before running integration tests.
 * It sets up the necessary environment properties for Spring Boot applications to use these containers.
 * </p>
 */
@Testcontainers
@ActiveProfiles("test")
class TestContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    /**
     * PostgreSQL Container instance.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2")).withReuse(true);

    /**
     * Kafka Container instance.
     */
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.4")).withReuse(true);

    /*
      Static block to start PostgreSQL and Kafka containers.
     */
    static {
        Startables.deepStart(postgres, kafka).join();
    }

    /**
     * Initializes the Spring application context with the necessary environment properties.
     *
     * @param ctx the configurable application context
     */
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword()
        ).applyTo(ctx.getEnvironment());
    }
}