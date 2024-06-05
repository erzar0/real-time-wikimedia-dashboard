package galactus.dashboard.repository;

import galactus.dashboard.entity.RecentchangeEventCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecentchangeEventCountRepository extends JpaRepository<RecentchangeEventCountEntity, UUID> {
}
