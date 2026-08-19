package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "diagnostic_imagings")
@SQLDelete(sql = "UPDATE diagnostic_imagings SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class DiagnosticImagingJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_imaging_type_id", nullable = false)
    private DiagnosticImagingTypeJpaEntity diagnosticImagingType;

    @Column(name = "clinical_signs", nullable = false, length = 2000)
    private String clinicalSigns;

    @Column(name = "study_type", nullable = false, length = 200)
    private String studyType;

    @Column(nullable = false, length = 2000)
    private String diagnosis;

    @Column(length = 2000)
    private String observations;

    @Column(nullable = false, length = 20)
    private String status;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected DiagnosticImagingJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public DiagnosticImagingTypeJpaEntity getDiagnosticImagingType() {
        return diagnosticImagingType;
    }

    public void setDiagnosticImagingType(DiagnosticImagingTypeJpaEntity diagnosticImagingType) {
        this.diagnosticImagingType = diagnosticImagingType;
    }

    public String getClinicalSigns() {
        return clinicalSigns;
    }

    public void setClinicalSigns(String clinicalSigns) {
        this.clinicalSigns = clinicalSigns;
    }

    public String getStudyType() {
        return studyType;
    }

    public void setStudyType(String studyType) {
        this.studyType = studyType;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AnimalJpaEntity getAnimal() {
        return animal;
    }

    public void setAnimal(AnimalJpaEntity animal) {
        this.animal = animal;
    }

    public ConsultationJpaEntity getConsultation() {
        return consultation;
    }

    public void setConsultation(ConsultationJpaEntity consultation) {
        this.consultation = consultation;
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
