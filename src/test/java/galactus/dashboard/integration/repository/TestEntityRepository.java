package galactus.dashboard.integration.repository;

import galactus.dashboard.integration.entity.TestEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestEntityRepository extends CrudRepository<TestEntity, UUID> { }