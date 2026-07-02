package com.vetsoftware.app.consultation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Consultation {
    private Long id;
    private LocalDate date;
    private ConsultationTypeRef consultationType;
    private String anamnesis;
    private String diagnosis;
    private String therapeuticPlan;
    private String diagnosisPlan;
    private LocalDate nextControl;
    private AnimalRef animal;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Consultation(Long id, LocalDate date, ConsultationTypeRef consultationType,
                        String anamnesis, String diagnosis, String therapeuticPlan,
                        String diagnosisPlan, LocalDate nextControl, AnimalRef animal,
                        CompanyRef company, LocalDateTime createdDate, boolean enabled) {
        validate(date, consultationType, anamnesis, diagnosis, therapeuticPlan, diagnosisPlan,
                 animal, company);
        this.id = id;
        this.date = date;
        this.consultationType = consultationType;
        this.anamnesis = anamnesis;
        this.diagnosis = blankToNull(diagnosis);
        this.therapeuticPlan = blankToNull(therapeuticPlan);
        this.diagnosisPlan = blankToNull(diagnosisPlan);
        this.nextControl = nextControl;
        this.animal = animal;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Consultation create(LocalDate date, ConsultationTypeRef consultationType,
                                      String anamnesis, String diagnosis, String therapeuticPlan,
                                      String diagnosisPlan, LocalDate nextControl,
                                      AnimalRef animal, CompanyRef company) {
        return new Consultation(null, date, consultationType, anamnesis, diagnosis,
                                therapeuticPlan, diagnosisPlan, nextControl, animal, company,
                                LocalDateTime.now(), true);
    }

    public void update(LocalDate date, ConsultationTypeRef consultationType,
                       String anamnesis, String diagnosis, String therapeuticPlan,
                       String diagnosisPlan, LocalDate nextControl, AnimalRef animal,
                       CompanyRef company) {
        validate(date, consultationType, anamnesis, diagnosis, therapeuticPlan, diagnosisPlan,
                 animal, company);
        this.date = date;
        this.consultationType = consultationType;
        this.anamnesis = anamnesis;
        this.diagnosis = blankToNull(diagnosis);
        this.therapeuticPlan = blankToNull(therapeuticPlan);
        this.diagnosisPlan = blankToNull(diagnosisPlan);
        this.nextControl = nextControl;
        this.animal = animal;
        this.company = company;
    }

    private static void validate(LocalDate date, ConsultationTypeRef consultationType,
                                  String anamnesis, String diagnosis, String therapeuticPlan,
                                  String diagnosisPlan, AnimalRef animal, CompanyRef company) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (consultationType == null) throw new IllegalArgumentException("consultationType is required");
        if (anamnesis == null || anamnesis.isBlank()) throw new IllegalArgumentException("anamnesis is required");
        if (anamnesis.length() > 2000) throw new IllegalArgumentException("anamnesis must be 2000 chars or less");
        // diagnosis, therapeuticPlan y diagnosisPlan son clínicamente opcionales
        // (p.ej. primera visita con dx pendiente): se aceptan null/vacío y solo se
        // valida el tope de longitud cuando vienen con contenido.
        if (diagnosis != null && diagnosis.length() > 2000) throw new IllegalArgumentException("diagnosis must be 2000 chars or less");
        if (therapeuticPlan != null && therapeuticPlan.length() > 2000) throw new IllegalArgumentException("therapeuticPlan must be 2000 chars or less");
        if (diagnosisPlan != null && diagnosisPlan.length() > 2000) throw new IllegalArgumentException("diagnosisPlan must be 2000 chars or less");
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public ConsultationTypeRef getConsultationType() { return consultationType; }
    public String getAnamnesis() { return anamnesis; }
    public String getDiagnosis() { return diagnosis; }
    public String getTherapeuticPlan() { return therapeuticPlan; }
    public String getDiagnosisPlan() { return diagnosisPlan; }
    public LocalDate getNextControl() { return nextControl; }
    public AnimalRef getAnimal() { return animal; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
