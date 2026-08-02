package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "procedure_schedules")
@SQLDelete(sql = "UPDATE procedure_schedules SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class ProcedureScheduleJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospitalization_procedure_id", nullable = false)
  private HospitalizationProcedureJpaEntity hospitalizationProcedure;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private EmployeeJpaEntity createdBy;

  @Column(name = "original_date_time", columnDefinition = "DATETIME")
  private LocalDateTime originalDateTime;

  @Column(name = "current_date_time", columnDefinition = "DATETIME")
  private LocalDateTime currentDateTime;

  @Column(name = "real_date_time", columnDefinition = "DATETIME")
  private LocalDateTime realDateTime;

  @Column(name = "applied_status", length = 40)
  private String appliedStatus;

  @Column(name = "rescheduled")
  private Boolean rescheduled;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected ProcedureScheduleJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public HospitalizationProcedureJpaEntity getHospitalizationProcedure() {
    return hospitalizationProcedure;
  }

  public void setHospitalizationProcedure(
      HospitalizationProcedureJpaEntity hospitalizationProcedure) {
    this.hospitalizationProcedure = hospitalizationProcedure;
  }

  public EmployeeJpaEntity getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(EmployeeJpaEntity createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getOriginalDateTime() {
    return originalDateTime;
  }

  public void setOriginalDateTime(LocalDateTime originalDateTime) {
    this.originalDateTime = originalDateTime;
  }

  public LocalDateTime getCurrentDateTime() {
    return currentDateTime;
  }

  public void setCurrentDateTime(LocalDateTime currentDateTime) {
    this.currentDateTime = currentDateTime;
  }

  public LocalDateTime getRealDateTime() {
    return realDateTime;
  }

  public void setRealDateTime(LocalDateTime realDateTime) {
    this.realDateTime = realDateTime;
  }

  public String getAppliedStatus() {
    return appliedStatus;
  }

  public void setAppliedStatus(String appliedStatus) {
    this.appliedStatus = appliedStatus;
  }

  public Boolean getRescheduled() {
    return rescheduled;
  }

  public void setRescheduled(Boolean rescheduled) {
    this.rescheduled = rescheduled;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
