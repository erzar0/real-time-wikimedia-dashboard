package galactus.dashboard.repository;

import galactus.dashboard.entity.RecentchangeLengthChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecentchangeLengthChangeRepository extends JpaRepository<RecentchangeLengthChangeEntity, UUID> {
}
