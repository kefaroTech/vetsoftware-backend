package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@SoftDelete(strategy = SoftDeleteType.ACTIVE, columnName = "enabled")
public class ConsultationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_type_id", nullable = false)
    private ConsultationTypeJpaEntity consultationType;

    @Column(nullable = false, length = 2000)
    private String anamnesis;

    @Column(nullable = false, length = 2000)
    private String diagnosis;

    @Column(name = "therapeutic_plan", nullable = false, length = 2000)
    private String therapeuticPlan;

    @Column(name = "diagnosis_plan", nullable = false, length = 2000)
    private String diagnosisPlan;

    @Column(name = "next_control")
    private LocalDate nextControl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private AnimalJpaEntity animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ConsultationJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ConsultationTypeJpaEntity getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationTypeJpaEntity consultationType) { this.consultationType = consultationType; }
    public String getAnamnesis() { return anamnesis; }
    public void setAnamnesis(String anamnesis) { this.anamnesis = anamnesis; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTherapeuticPlan() { return therapeuticPlan; }
    public void setTherapeuticPlan(String therapeuticPlan) { this.therapeuticPlan = therapeuticPlan; }
    public String getDiagnosisPlan() { return diagnosisPlan; }
    public void setDiagnosisPlan(String diagnosisPlan) { this.diagnosisPlan = diagnosisPlan; }
    public LocalDate getNextControl() { return nextControl; }
    public void setNextControl(LocalDate nextControl) { this.nextControl = nextControl; }
    public AnimalJpaEntity getAnimal() { return animal; }
    public void setAnimal(AnimalJpaEntity animal) { this.animal = animal; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
