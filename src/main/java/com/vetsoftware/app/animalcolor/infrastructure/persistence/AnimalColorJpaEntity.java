package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "animal_colors", uniqueConstraints = {
        @UniqueConstraint(name = "uq_animal_colors_specie_name", columnNames = {"specie_id",
                "name"})})
@SQLDelete(sql = "UPDATE animal_colors SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class AnimalColorJpaEntity {
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

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected AnimalColorJpaEntity() {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
