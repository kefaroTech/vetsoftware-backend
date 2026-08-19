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
    private LaboratoryTestPriority prioridad;
    private AnimalRef animal;
    private ConsultationRef consultation;
    private CompanyRef company;
    private Long branchId;
    private EmployeeRef processedBy;
    private LocalDateTime processedDate;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public LaboratoryTest(Long id, LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
            String diagnosis, LaboratoryTestStatus status, LaboratoryTestPriority prioridad,
            AnimalRef animal, ConsultationRef consultation, CompanyRef company, Long branchId,
            EmployeeRef processedBy, LocalDateTime processedDate, LocalDateTime createdDate,
            Long version, boolean enabled) {
        validate(date, testType, quantity, diagnosis, status, prioridad, animal, consultation,
                company, branchId);
        this.id = id;
        this.date = date;
        this.testType = testType;
        this.quantity = quantity;
        this.diagnosis = diagnosis;
        this.status = status;
        this.prioridad = prioridad;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.branchId = branchId;
        this.processedBy = processedBy;
        this.processedDate = processedDate;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static LaboratoryTest create(LocalDate date, LaboratoryTestTypeRef testType,
            Integer quantity, String diagnosis, AnimalRef animal, ConsultationRef consultation,
            CompanyRef company, Long branchId) {
        return create(date, testType, quantity, diagnosis, LaboratoryTestStatus.PENDING_COLLECTION,
                LaboratoryTestPriority.NORMAL, animal, consultation, company, branchId, null, null);
    }

    public static LaboratoryTest create(LocalDate date, LaboratoryTestTypeRef testType,
            Integer quantity, String diagnosis, LaboratoryTestStatus initialStatus,
            LaboratoryTestPriority prioridad, AnimalRef animal, ConsultationRef consultation,
            CompanyRef company, Long branchId, EmployeeRef processedBy,
            LocalDateTime processedDate) {
        if (initialStatus != LaboratoryTestStatus.PENDING_COLLECTION
                && initialStatus != LaboratoryTestStatus.PENDING_PROCESSING) {
            throw new IllegalArgumentException(
                    "initial status must be PENDING_COLLECTION or PENDING_PROCESSING");
        }
        return new LaboratoryTest(null, date, testType, quantity, diagnosis, initialStatus,
                prioridad, animal, consultation, company, branchId, processedBy, processedDate,
                LocalDateTime.now(), null, true);
    }

    public void update(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
            String diagnosis, LaboratoryTestPriority prioridad, AnimalRef animal,
            ConsultationRef consultation, CompanyRef company, EmployeeRef processedBy,
            LocalDateTime processedDate) {
        validate(date, testType, quantity, diagnosis, this.status, prioridad, animal, consultation,
                company, this.branchId);
        this.date = date;
        this.testType = testType;
        this.quantity = quantity;
        this.diagnosis = diagnosis;
        this.prioridad = prioridad;
        this.animal = animal;
        this.consultation = consultation;
        this.company = company;
        this.processedBy = processedBy;
        this.processedDate = processedDate;
    }

    public void changeStatus(LaboratoryTestStatus newStatus) {
        if (newStatus == null)
            throw new IllegalArgumentException("status is required");
        this.status = newStatus;
    }

    /**
     * Cambia el estado registrando quién lo procesó/validó y cuándo (firma al
     * validar).
     */
    public void changeStatus(LaboratoryTestStatus newStatus, EmployeeRef processedBy,
            LocalDateTime processedDate) {
        changeStatus(newStatus);
        this.processedBy = processedBy;
        this.processedDate = processedDate;
    }

    private static void validate(LocalDate date, LaboratoryTestTypeRef testType, Integer quantity,
            String diagnosis, LaboratoryTestStatus status, LaboratoryTestPriority prioridad,
            AnimalRef animal, ConsultationRef consultation, CompanyRef company, Long branchId) {
        if (date == null)
            throw new IllegalArgumentException("date is required");
        if (testType == null)
            throw new IllegalArgumentException("testType is required");
        if (quantity == null)
            throw new IllegalArgumentException("quantity is required");
        if (quantity < 1)
            throw new IllegalArgumentException("quantity must be at least 1");
        if (diagnosis != null && diagnosis.length() > 2000)
            throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (prioridad == null)
            throw new IllegalArgumentException("prioridad is required");
        if (animal == null)
            throw new IllegalArgumentException("animal is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (branchId == null)
            throw new IllegalArgumentException("branch is required");
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LaboratoryTestTypeRef getTestType() {
        return testType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public LaboratoryTestStatus getStatus() {
        return status;
    }

    public LaboratoryTestPriority getPrioridad() {
        return prioridad;
    }

    public AnimalRef getAnimal() {
        return animal;
    }

    public ConsultationRef getConsultation() {
        return consultation;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public Long getBranchId() {
        return branchId;
    }

    public EmployeeRef getProcessedBy() {
        return processedBy;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
