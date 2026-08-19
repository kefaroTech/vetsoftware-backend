package com.vetsoftware.app.breed.infrastructure.persistence;

import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "breeds", uniqueConstraints = {
        @UniqueConstraint(name = "uq_breeds_specie_name", columnNames = {"specie_id", "name"})})
@SQLDelete(sql = "UPDATE breeds SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class BreedJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specie_id", nullable = false)
    private SpecieJpaEntity specie;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected BreedJpaEntity() {
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

    public SpecieJpaEntity getSpecie() {
        return specie;
    }

    public void setSpecie(SpecieJpaEntity specie) {
        this.specie = specie;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
