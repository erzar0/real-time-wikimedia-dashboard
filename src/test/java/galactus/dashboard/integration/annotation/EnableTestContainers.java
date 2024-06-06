package galactus.dashboard.integration.annotation;

import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to enable TestContainers support in Spring tests.
 * <p>
 * This annotation can be used on test classes to initialize TestContainers with a specified initializer class.
 * </p>
 * <p>
 * The {@link ContextConfiguration} annotation is used to specify the class that initializes the TestContainers.
 * </p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(initializers = TestContainersInitializer.class)
public @interface EnableTestContainers {
}