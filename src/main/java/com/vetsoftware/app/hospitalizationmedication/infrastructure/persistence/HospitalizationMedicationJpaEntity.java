package com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "hospitalization_medications")
@SQLDelete(sql = "UPDATE hospitalization_medications SET enabled = false"
        + " WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class HospitalizationMedicationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String dose;

    @Column(name = "frequency", length = 40)
    private String frequency;

    @Column(name = "guideline_type", length = 40)
    private String guidelineType;

    @Column(name = "duration_measure", length = 40)
    private String durationMeasure;

    @Column(name = "duration_quantity")
    private Integer durationQuantity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "notes", length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalization_id", nullable = false)
    private HospitalizationJpaEntity hospitalization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private EmployeeJpaEntity createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "suspension_date")
    private LocalDateTime suspensionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspension_by_id")
    private EmployeeJpaEntity suspensionBy;

    protected HospitalizationMedicationJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getGuidelineType() {
        return guidelineType;
    }

    public void setGuidelineType(String guidelineType) {
        this.guidelineType = guidelineType;
    }

    public String getDurationMeasure() {
        return durationMeasure;
    }

    public void setDurationMeasure(String durationMeasure) {
        this.durationMeasure = durationMeasure;
    }

    public Integer getDurationQuantity() {
        return durationQuantity;
    }

    public void setDurationQuantity(Integer durationQuantity) {
        this.durationQuantity = durationQuantity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public HospitalizationJpaEntity getHospitalization() {
        return hospitalization;
    }

    public void setHospitalization(HospitalizationJpaEntity hospitalization) {
        this.hospitalization = hospitalization;
    }

    public EmployeeJpaEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(EmployeeJpaEntity createdBy) {
        this.createdBy = createdBy;
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

    public LocalDateTime getSuspensionDate() {
        return suspensionDate;
    }

    public void setSuspensionDate(LocalDateTime suspensionDate) {
        this.suspensionDate = suspensionDate;
    }

    public EmployeeJpaEntity getSuspensionBy() {
        return suspensionBy;
    }

    public void setSuspensionBy(EmployeeJpaEntity suspensionBy) {
        this.suspensionBy = suspensionBy;
    }
}
