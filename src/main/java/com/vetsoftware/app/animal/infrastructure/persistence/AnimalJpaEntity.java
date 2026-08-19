package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "animals")
@SQLDelete(sql = "UPDATE animals SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class AnimalJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specie_id", nullable = false)
    private SpecieJpaEntity specie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", nullable = false)
    private BreedJpaEntity breed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerJpaEntity owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_type", nullable = false, length = 20)
    private WeightType weightType;

    @Enumerated(EnumType.STRING)
    @Column(name = "animal_type", nullable = false, length = 20)
    private AnimalType animalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reproductive_state", nullable = false, length = 20)
    private ReproductiveState reproductiveState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private AnimalColorJpaEntity color;

    @Column(name = "bod")
    private LocalDate bod;

    @Column(name = "size")
    private Integer size;

    @Column(name = "deceased", nullable = false)
    private boolean deceased;

    @Column(name = "deceased_date")
    private LocalDate deceasedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected AnimalJpaEntity() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public SpecieJpaEntity getSpecie() {
        return specie;
    }

    public void setSpecie(SpecieJpaEntity specie) {
        this.specie = specie;
    }

    public BreedJpaEntity getBreed() {
        return breed;
    }

    public void setBreed(BreedJpaEntity breed) {
        this.breed = breed;
    }

    public OwnerJpaEntity getOwner() {
        return owner;
    }

    public void setOwner(OwnerJpaEntity owner) {
        this.owner = owner;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public WeightType getWeightType() {
        return weightType;
    }

    public void setWeightType(WeightType weightType) {
        this.weightType = weightType;
    }

    public AnimalType getAnimalType() {
        return animalType;
    }

    public void setAnimalType(AnimalType animalType) {
        this.animalType = animalType;
    }

    public ReproductiveState getReproductiveState() {
        return reproductiveState;
    }

    public void setReproductiveState(ReproductiveState reproductiveState) {
        this.reproductiveState = reproductiveState;
    }

    public AnimalColorJpaEntity getColor() {
        return color;
    }

    public void setColor(AnimalColorJpaEntity color) {
        this.color = color;
    }

    public LocalDate getBod() {
        return bod;
    }

    public void setBod(LocalDate bod) {
        this.bod = bod;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public boolean isDeceased() {
        return deceased;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    public LocalDate getDeceasedDate() {
        return deceasedDate;
    }

    public void setDeceasedDate(LocalDate deceasedDate) {
        this.deceasedDate = deceasedDate;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
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
