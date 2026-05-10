package com.vetsoftware.app.laboratorytest.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LaboratoryTest {
    private Long id;
    private LocalDate date;
    private LaboratoryTestTypeRef testType;
    private Integer quantity;
    private String diagnosis;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private final LocalDateTime createdDate;

    public LaboratoryTest(Long id, LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                          String diagnosis, AnimalRef animal, ConsultationRef consultation,
                          CompanyRef company, LocalDateTime createdDate) {
        validate(date, testType, quantity, diagnosis, animal, consultation, company);
        this.id = id;
        this.date = date;
        this.testType = testType;
        this.quantity = quantity;
        this.diagnosis = diagnosis;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.createdDate = createdDate;
    }

    public static LaboratoryTest create(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                                        String diagnosis, AnimalRef animal,
                                        ConsultationRef consultation, CompanyRef company) {
        return new LaboratoryTest(null, date, testType, quantity, diagnosis, animal, consultation,
                                  company, LocalDateTime.now());
    }

    public void update(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                       String diagnosis, AnimalRef animal, ConsultationRef consultation,
                       CompanyRef company) {
        validate(date, testType, quantity, diagnosis, animal, consultation, company);
        this.date = date;
        this.testType = testType;
        this.quantity = quantity;
        this.diagnosis = diagnosis;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
    }

    private static void validate(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
                                  String diagnosis, AnimalRef animal, ConsultationRef consultation,
                                  CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (testType == null) throw new IllegalArgumentException("testType is required");
        if (quantity == null) throw new IllegalArgumentException("quantity is required");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be at least 1");
        if (diagnosis == null || diagnosis.isBlank()) throw new IllegalArgumentException("diagnosis is required");
        if (diagnosis.length() > 2000) throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LaboratoryTestTypeRef getTestType() { return testType; }
    public Integer getQuantity() { return quantity; }
    public String getDiagnosis() { return diagnosis; }
    public AnimalRef getAnimal() { return animal; }
    public ConsultationRef getConsultation() { return consultation; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
