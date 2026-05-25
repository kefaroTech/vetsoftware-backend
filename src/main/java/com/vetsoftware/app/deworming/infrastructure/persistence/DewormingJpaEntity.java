package com.vetsoftware.app.deworming.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.deworming.domain.DewormingType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "dewormings")
@SoftDelete(strategy = SoftDeleteType.ACTIVE, columnName = "enabled")
public class DewormingJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "last_deworming")
    private LocalDate lastDeworming;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DewormingType type;

    @Column(nullable = false, length = 200)
    private String product;

    @Column(nullable = false, length = 200)
    private String dosage;

    @Column(name = "next_control")
    private LocalDate nextControl;

    @Column(length = 2000)
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private AnimalJpaEntity animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private ConsultationJpaEntity consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected DewormingJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalDate getLastDeworming() { return lastDeworming; }
    public void setLastDeworming(LocalDate lastDeworming) { this.lastDeworming = lastDeworming; }
    public DewormingType getType() { return type; }
    public void setType(DewormingType type) { this.type = type; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public LocalDate getNextControl() { return nextControl; }
    public void setNextControl(LocalDate nextControl) { this.nextControl = nextControl; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public AnimalJpaEntity getAnimal() { return animal; }
    public void setAnimal(AnimalJpaEntity animal) { this.animal = animal; }
    public ConsultationJpaEntity getConsultation() { return consultation; }
    public void setConsultation(ConsultationJpaEntity consultation) { this.consultation = consultation; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
