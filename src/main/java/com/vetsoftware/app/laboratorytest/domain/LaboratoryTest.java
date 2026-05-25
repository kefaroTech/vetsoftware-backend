package com.vetsoftware.app.laboratorytest.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LaboratoryTest {
    private Long id;
    private LocalDate date;
    private LaboratoryTestTypeRef testType;
    private Integer quantity;
    private String diagnosis;
    private LaboratoryTestStatus status;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public LaboratoryTest(Long id, LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                          String diagnosis, LaboratoryTestStatus status, AnimalRef animal,
                          ConsultationRef consultation, CompanyRef company, LocalDateTime createdDate,
                          boolean enabled) {
        validate(date, testType, quantity, diagnosis, status, animal, consultation, company);
        this.id = id;
        this.date = date;
        this.testType = testType;
        this.quantity = quantity;
        this.diagnosis = diagnosis;
        this.status = status;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static LaboratoryTest create(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                                        String diagnosis, AnimalRef animal,
                                        ConsultationRef consultation, CompanyRef company) {
        return create(date, testType, quantity, diagnosis, LaboratoryTestStatus.PENDIENTE,
                      animal, consultation, company);
    }

    public static LaboratoryTest create(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                                        String diagnosis, LaboratoryTestStatus initialStatus,
                                        AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        if (initialStatus != LaboratoryTestStatus.PENDIENTE
                && initialStatus != LaboratoryTestStatus.PENDIENTE_POR_PROCESAR) {
            throw new IllegalArgumentException(
                "initial status must be PENDIENTE or PENDIENTE_POR_PROCESAR");
        }
        return new LaboratoryTest(null, date, testType, quantity, diagnosis,
                                  initialStatus, animal, consultation, company,
                                  LocalDateTime.now(), true);
    }

    public void update(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                       String diagnosis, AnimalRef animal, ConsultationRef consultation,
                       CompanyRef company) {
        validate(date, testType, quantity, diagnosis, this.status, animal, consultation, company);
        this.date = date;
        this.testType = testType;
        this.quantity = quantity;
        this.diagnosis = diagnosis;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    public void changeStatus(LaboratoryTestStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("status is required");
        this.status = newStatus;
    }

    private static void validate(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                                  String diagnosis, LaboratoryTestStatus status, AnimalRef animal,
                                  ConsultationRef consultation, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (testType == null) throw new IllegalArgumentException("testType is required");
        if (quantity == null) throw new IllegalArgumentException("quantity is required");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be at least 1");
        if (diagnosis == null || diagnosis.isBlank()) throw new IllegalArgumentException("diagnosis is required");
        if (diagnosis.length() > 2000) throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LaboratoryTestTypeRef getTestType() { return testType; }
    public Integer getQuantity() { return quantity; }
    public String getDiagnosis() { return diagnosis; }
    public LaboratoryTestStatus getStatus() { return status; }
    public AnimalRef getAnimal() { return animal; }
    public ConsultationRef getConsultation() { return consultation; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
