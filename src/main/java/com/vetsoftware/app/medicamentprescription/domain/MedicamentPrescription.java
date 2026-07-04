package com.vetsoftware.app.medicamentprescription.domain;

import java.time.LocalDateTime;

public class MedicamentPrescription {
    private Long id;
    private String name;
    private String presentation;
    private Double quantity;
    private String posology;
    private String observation;
    private PrescriptionRef prescription;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public MedicamentPrescription(Long id, String name, String presentation, Double quantity,
                                  String posology, String observation, PrescriptionRef prescription,
                                  LocalDateTime createdDate, boolean enabled) {
        validate(name, presentation, quantity, posology, observation, prescription);
        this.id = id;
        this.name = name;
        this.presentation = presentation;
        this.quantity = quantity;
        this.posology = posology;
        this.observation = blankToNull(observation);
        this.prescription = prescription;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static MedicamentPrescription create(String name, String presentation, Double quantity,
                                                String posology, String observation, PrescriptionRef prescription) {
        return new MedicamentPrescription(null, name, presentation, quantity, posology, observation,
                                          prescription, LocalDateTime.now(), true);
    }

    public void update(String name, String presentation, Double quantity, String posology,
                       String observation, PrescriptionRef prescription) {
        validate(name, presentation, quantity, posology, observation, prescription);
        this.name = name;
        this.presentation = presentation;
        this.quantity = quantity;
        this.posology = posology;
        this.observation = blankToNull(observation);
        this.prescription = prescription;
    }

    public void enable() { this.enabled = true; }

    public void disable() { this.enabled = false; }

    private static void validate(String name, String presentation, Double quantity,
                                  String posology, String observation, PrescriptionRef prescription) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 200) throw new IllegalArgumentException("name must be 200 chars or less");
        if (presentation == null || presentation.isBlank()) throw new IllegalArgumentException("presentation is required");
        if (presentation.length() > 200) throw new IllegalArgumentException("presentation must be 200 chars or less");
        if (quantity == null) throw new IllegalArgumentException("quantity is required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (posology == null || posology.isBlank()) throw new IllegalArgumentException("posology is required");
        if (posology.length() > 1000) throw new IllegalArgumentException("posology must be 1000 chars or less");
        // observation es opcional; solo se valida el tope de longitud cuando viene con contenido.
        if (observation != null && observation.length() > 1000) throw new IllegalArgumentException("observation must be 1000 chars or less");
        if (prescription == null) throw new IllegalArgumentException("prescription is required");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPresentation() { return presentation; }
    public Double getQuantity() { return quantity; }
    public String getPosology() { return posology; }
    public String getObservation() { return observation; }
    public PrescriptionRef getPrescription() { return prescription; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
}
