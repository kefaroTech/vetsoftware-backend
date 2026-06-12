package com.vetsoftware.app.city.infrastructure.persistence;

import com.vetsoftware.app.state.infrastructure.persistence.StateJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "cities", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cities_state_name", columnNames = {"state_id", "name"})
})
@SQLDelete(sql = "UPDATE cities SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class CityJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private StateJpaEntity state;

    @Column(name = "dane_code", length = 5)
    private String daneCode;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CityJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public StateJpaEntity getState() { return state; }
    public void setState(StateJpaEntity state) { this.state = state; }
    public String getDaneCode() { return daneCode; }
    public void setDaneCode(String daneCode) { this.daneCode = daneCode; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
