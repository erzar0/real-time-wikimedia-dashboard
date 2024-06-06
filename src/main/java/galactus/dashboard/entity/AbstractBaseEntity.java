package galactus.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for entities, providing common properties such as ID, creation timestamp, and update timestamp.
 * <p>
 * This class is intended to be extended by other entity classes.
 * It includes:
 * <ul>
 *     <li>An auto-generated UUID as the primary key.</li>
 *     <li>A creation timestamp, set automatically when the entity is persisted.</li>
 *     <li>An update timestamp, set automatically whenever the entity is updated.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
public class AbstractBaseEntity {
    /**
     * The unique identifier for the entity.
     * This ID is generated automatically using the UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The timestamp when the entity was created.
     * This is set automatically and is not updatable.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The timestamp when the entity was last updated.
     * This is set automatically whenever the entity is updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}