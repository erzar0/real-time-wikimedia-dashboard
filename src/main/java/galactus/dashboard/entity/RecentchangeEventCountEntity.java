package galactus.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Entity
@Table(name = "recentchange_event_count_entity")
public class RecentchangeEventCountEntity extends AbstractBaseEntity{
    @Column(name = "value", nullable = false)
    int value;
}
