package galactus.dashboard.repository;

import galactus.dashboard.entity.RecentchangeActiveUsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public interface RecentchangeActiveUsersRepository extends JpaRepository<RecentchangeActiveUsersEntity, UUID> {
    Optional<RecentchangeActiveUsersEntity> findTopByOrderByCreatedAtDesc();
}
