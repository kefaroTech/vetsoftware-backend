package com.vetsoftware.app.medicamentprescription.domain;

import java.time.LocalDateTime;

public class MedicamentPrescription {
    private Long id;
    private String name;
    private String presentation;
    private Double quantity;
    private String posology;
    private PrescriptionRef prescription;
    private final LocalDateTime createdDate;

    public MedicamentPrescription(Long id, String name, String presentation, Double quantity,
                                  String posology, PrescriptionRef prescription,
                                  LocalDateTime createdDate) {
        validate(name, presentation, quantity, posology, prescription);
        this.id = id;
        this.name = name;
        this.presentation = presentation;
        this.quantity = quantity;
        this.posology = posology;
        this.prescription = prescription;
        this.createdDate = createdDate;
    }

    public static MedicamentPrescription create(String name, String presentation, Double quantity,
                                                String posology, PrescriptionRef prescription) {
        return new MedicamentPrescription(null, name, presentation, quantity, posology,
                                          prescription, LocalDateTime.now());
    }

    public void update(String name, String presentation, Double quantity, String posology,
                       PrescriptionRef prescription) {
        validate(name, presentation, quantity, posology, prescription);
        this.name = name;
        this.presentation = presentation;
        this.quantity = quantity;
        this.posology = posology;
        this.prescription = prescription;
    }

    private static void validate(String name, String presentation, Double quantity,
                                  String posology, PrescriptionRef prescription) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 200) throw new IllegalArgumentException("name must be 200 chars or less");
        if (presentation == null || presentation.isBlank()) throw new IllegalArgumentException("presentation is required");
        if (presentation.length() > 200) throw new IllegalArgumentException("presentation must be 200 chars or less");
        if (quantity == null) throw new IllegalArgumentException("quantity is required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (posology == null || posology.isBlank()) throw new IllegalArgumentException("posology is required");
        if (posology.length() > 1000) throw new IllegalArgumentException("posology must be 1000 chars or less");
        if (prescription == null) throw new IllegalArgumentException("prescription is required");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPresentation() { return presentation; }
    public Double getQuantity() { return quantity; }
    public String getPosology() { return posology; }
    public PrescriptionRef getPrescription() { return prescription; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
