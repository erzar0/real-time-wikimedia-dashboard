package galactus.dashboard.integration.entity;

import galactus.dashboard.entity.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestEntity extends AbstractBaseEntity {

    @Column(name = "field")
    private String field;
}
