package galactus.dashboard.integration;

import galactus.dashboard.DashboardApplication;
import galactus.dashboard.integration.annotation.EnableTestContainers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Abstract base class for integration tests with TestContainers support.
 * <p>
 * This class is intended to be extended by integration test classes.
 * It configures the Spring Boot application context for testing by specifying:
 * - The main application class ({@link DashboardApplication}) to load the application context.
 * - The web environment as {@link SpringBootTest.WebEnvironment.RANDOM_PORT} to start the application in a random port.
 * - Activates the "test" profile to use test-specific configurations.
 * - Enables TestContainers support using the {@link EnableTestContainers} annotation.
 * </p>
 */

@SpringBootTest(
        classes = DashboardApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@EnableTestContainers
abstract public class AbstractIntegrationTest { }
