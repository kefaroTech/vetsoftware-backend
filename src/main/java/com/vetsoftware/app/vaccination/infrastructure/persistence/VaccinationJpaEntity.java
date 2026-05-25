package com.vetsoftware.app.vaccination.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vaccinations")
@SQLDelete(sql = "UPDATE vaccinations SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class VaccinationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccination_type_id", nullable = false)
    private VaccinationTypeJpaEntity vaccinationType;

    @Column(nullable = false, length = 100)
    private String lot;

    @Column(length = 2000)
    private String notes;

    @Column(name = "next_vaccination")
    private LocalDate nextVaccination;

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

    protected VaccinationJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public VaccinationTypeJpaEntity getVaccinationType() { return vaccinationType; }
    public void setVaccinationType(VaccinationTypeJpaEntity vaccinationType) { this.vaccinationType = vaccinationType; }
    public String getLot() { return lot; }
    public void setLot(String lot) { this.lot = lot; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getNextVaccination() { return nextVaccination; }
    public void setNextVaccination(LocalDate nextVaccination) { this.nextVaccination = nextVaccination; }
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
