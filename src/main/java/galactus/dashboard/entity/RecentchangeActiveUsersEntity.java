package galactus.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Entity
@Table(name = "recentchange_active_users_entity")
public class RecentchangeActiveUsersEntity extends AbstractBaseEntity{
    @Lob
    @Column(name = "value", nullable = false, columnDefinition="text")
    String value;
}
