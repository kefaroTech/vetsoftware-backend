package com.vetsoftware.app.state.infrastructure.persistence;

import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "states", uniqueConstraints = {
        @UniqueConstraint(name = "uq_states_country_name", columnNames = {"country_id", "name"})})
@SQLDelete(sql = "UPDATE states SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class StateJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryJpaEntity country;

    @Column(name = "dane_code", length = 2)
    private String daneCode;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected StateJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CountryJpaEntity getCountry() {
        return country;
    }

    public void setCountry(CountryJpaEntity country) {
        this.country = country;
    }

    public String getDaneCode() {
        return daneCode;
    }

    public void setDaneCode(String daneCode) {
        this.daneCode = daneCode;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
