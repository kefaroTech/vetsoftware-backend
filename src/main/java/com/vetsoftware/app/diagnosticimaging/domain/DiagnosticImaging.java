package com.vetsoftware.app.diagnosticimaging.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DiagnosticImaging {
    private Long id;
    private LocalDate date;
    private DiagnosticImagingTypeRef diagnosticImagingType;
    private String clinicalSigns;
    private String studyType;
    private String diagnosis;
    private String observations;
    private DiagnosticImagingStatus status;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;

    public DiagnosticImaging(Long id, LocalDate date, DiagnosticImagingTypeRef diagnosticImagingType,
                             String clinicalSigns, String studyType, String diagnosis, String observations,
                             DiagnosticImagingStatus status, AnimalRef animal,
                             ConsultationRef consultation, CompanyRef company,
                             LocalDateTime createdDate) {
        validate(date, diagnosticImagingType, clinicalSigns, studyType, diagnosis, observations,
                 status, animal, company);
        this.id = id;
        this.date = date;
        this.diagnosticImagingType = diagnosticImagingType;
        this.clinicalSigns = clinicalSigns;
        this.studyType = studyType;
        this.diagnosis = diagnosis;
        this.observations = observations;
        this.status = status;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
    }

    public static DiagnosticImaging create(LocalDate date, DiagnosticImagingTypeRef diagnosticImagingType,
                                           String clinicalSigns, String studyType, String diagnosis,
                                           String observations, AnimalRef animal,
                                           ConsultationRef consultation, CompanyRef company) {
        return new DiagnosticImaging(null, date, diagnosticImagingType, clinicalSigns, studyType,
                                     diagnosis, observations, DiagnosticImagingStatus.PENDIENTE,
                                     animal, consultation, company, LocalDateTime.now());
    }

    public void update(LocalDate date, DiagnosticImagingTypeRef diagnosticImagingType,
                       String clinicalSigns, String studyType, String diagnosis, String observations,
                       AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        validate(date, diagnosticImagingType, clinicalSigns, studyType, diagnosis, observations,
                 this.status, animal, company);
        this.date = date;
        this.diagnosticImagingType = diagnosticImagingType;
        this.clinicalSigns = clinicalSigns;
        this.studyType = studyType;
        this.diagnosis = diagnosis;
        this.observations = observations;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    public void changeStatus(DiagnosticImagingStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("status is required");
        this.status = newStatus;
    }

    private static void validate(LocalDate date, DiagnosticImagingTypeRef diagnosticImagingType,
                                  String clinicalSigns, String studyType, String diagnosis,
                                  String observations, DiagnosticImagingStatus status,
                                  AnimalRef animal, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (diagnosticImagingType == null) throw new IllegalArgumentException("diagnosticImagingType is required");
        if (clinicalSigns == null || clinicalSigns.isBlank()) throw new IllegalArgumentException("clinicalSigns is required");
        if (clinicalSigns.length() > 2000) throw new IllegalArgumentException("clinicalSigns must be 2000 chars or less");
        if (studyType == null || studyType.isBlank()) throw new IllegalArgumentException("studyType is required");
        if (studyType.length() > 200) throw new IllegalArgumentException("studyType must be 200 chars or less");
        if (diagnosis == null || diagnosis.isBlank()) throw new IllegalArgumentException("diagnosis is required");
        if (diagnosis.length() > 2000) throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
        if (observations != null && observations.length() > 2000)
            throw new IllegalArgumentException("observations must be 2000 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public DiagnosticImagingTypeRef getDiagnosticImagingType() { return diagnosticImagingType; }
    public String getClinicalSigns() { return clinicalSigns; }
    public String getStudyType() { return studyType; }
    public String getDiagnosis() { return diagnosis; }
    public String getObservations() { return observations; }
    public DiagnosticImagingStatus getStatus() { return status; }
    public AnimalRef getAnimal() { return animal; }
    public ConsultationRef getConsultation() { return consultation; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
