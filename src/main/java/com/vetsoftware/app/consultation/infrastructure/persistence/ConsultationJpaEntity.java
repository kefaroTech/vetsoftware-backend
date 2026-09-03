package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "consultations")
@SQLDelete(sql = "UPDATE consultations SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
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

    @Column(length = 2000)
    private String diagnosis;

    @Column(name = "prognosis", length = 500)
    private String prognosis;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "mucous_membranes", length = 40)
    private String mucousMembranes;

    @Column(name = "capillary_refill", length = 20)
    private String capillaryRefill;

    @Column(name = "hydration", length = 20)
    private String hydration;

    @Column(name = "body_condition_score")
    private Integer bodyConditionScore;

    @Column(name = "pain_score")
    private Integer painScore;

    @Column(name = "attitude", length = 40)
    private String attitude;

    @Column(name = "exam_findings", length = 2000)
    private String examFindings;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ConsultationJpaEntity() {
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

    public ConsultationTypeJpaEntity getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationTypeJpaEntity consultationType) {
        this.consultationType = consultationType;
    }

    public String getAnamnesis() {
        return anamnesis;
    }

    public void setAnamnesis(String anamnesis) {
        this.anamnesis = anamnesis;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrognosis() {
        return prognosis;
    }

    public void setPrognosis(String prognosis) {
        this.prognosis = prognosis;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public String getMucousMembranes() {
        return mucousMembranes;
    }

    public void setMucousMembranes(String mucousMembranes) {
        this.mucousMembranes = mucousMembranes;
    }

    public String getCapillaryRefill() {
        return capillaryRefill;
    }

    public void setCapillaryRefill(String capillaryRefill) {
        this.capillaryRefill = capillaryRefill;
    }

    public String getHydration() {
        return hydration;
    }

    public void setHydration(String hydration) {
        this.hydration = hydration;
    }

    public Integer getBodyConditionScore() {
        return bodyConditionScore;
    }

    public void setBodyConditionScore(Integer bodyConditionScore) {
        this.bodyConditionScore = bodyConditionScore;
    }

    public Integer getPainScore() {
        return painScore;
    }

    public void setPainScore(Integer painScore) {
        this.painScore = painScore;
    }

    public String getAttitude() {
        return attitude;
    }

    public void setAttitude(String attitude) {
        this.attitude = attitude;
    }

    public String getExamFindings() {
        return examFindings;
    }

    public void setExamFindings(String examFindings) {
        this.examFindings = examFindings;
    }

    public LocalDate getNextControl() {
        return nextControl;
    }

    public void setNextControl(LocalDate nextControl) {
        this.nextControl = nextControl;
    }

    public AnimalJpaEntity getAnimal() {
        return animal;
    }

    public void setAnimal(AnimalJpaEntity animal) {
        this.animal = animal;
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
