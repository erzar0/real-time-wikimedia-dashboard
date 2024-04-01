package galactus.dashboard.integration.repository;

import galactus.dashboard.integration.AbstractIntegrationTest;
import galactus.dashboard.integration.entity.TestEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


public class TestEntityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Test
    public void testEntityShouldBeSavedAndRetrieved() {
        // Given
        TestEntity testEntity = new TestEntity("test field");

        // When
        TestEntity savedEntity = testEntityRepository.save(testEntity);
        TestEntity retrievedEntity = testEntityRepository.findById(savedEntity.getId()).orElse(null);

        // Then
        assertThat(retrievedEntity).isNotNull();
        assertThat(retrievedEntity.getField()).isEqualTo("test field");
    }

    @Test
    public void testEntityShouldBeUpdated() {
        // Given
        TestEntity testEntity = new TestEntity("test field");
        TestEntity savedEntity = testEntityRepository.save(testEntity);

        // When
        savedEntity.setField("updated field");
        TestEntity updatedEntity = testEntityRepository.save(savedEntity);

        // Then
        assertThat(updatedEntity.getField()).isEqualTo("updated field");
    }

    @Test
    public void testEntityShouldBeDeleted() {
        // Given
        TestEntity testEntity = new TestEntity("test field");
        TestEntity savedEntity = testEntityRepository.save(testEntity);

        // When
        testEntityRepository.deleteById(savedEntity.getId());

        // Then
        Assertions.assertThat(testEntityRepository.findById(savedEntity.getId())).isEmpty();
    }
}

