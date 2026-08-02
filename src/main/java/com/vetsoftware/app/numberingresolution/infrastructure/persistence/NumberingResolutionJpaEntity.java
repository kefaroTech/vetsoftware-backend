package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "numbering_resolutions")
@SQLDelete(sql = "UPDATE numbering_resolutions SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class NumberingResolutionJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyJpaEntity company;

  // Multi-sucursal (B-6): sede a la que aplica el prefijo. Id pelado (sin @ManyToOne, como
  // electronic_documents.branch_id). null = resolución de empresa (todas las sedes).
  @Column(name = "branch_id")
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 30)
  private ElectronicDocumentType documentType;

  @Column(name = "resolution_number", nullable = false, length = 50)
  private String resolutionNumber;

  @Column(name = "resolution_date", nullable = false)
  private LocalDate resolutionDate;

  @Column(name = "prefix", length = 10)
  private String prefix;

  @Column(name = "range_from", nullable = false)
  private Long rangeFrom;

  @Column(name = "range_to", nullable = false)
  private Long rangeTo;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = false)
  private LocalDate validTo;

  @Column(name = "technical_key", length = 255)
  private String technicalKey;

  @Column(name = "current_number", nullable = false)
  private Long currentNumber;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected NumberingResolutionJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CompanyJpaEntity getCompany() {
    return company;
  }

  public void setCompany(CompanyJpaEntity company) {
    this.company = company;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public ElectronicDocumentType getDocumentType() {
    return documentType;
  }

  public void setDocumentType(ElectronicDocumentType documentType) {
    this.documentType = documentType;
  }

  public String getResolutionNumber() {
    return resolutionNumber;
  }

  public void setResolutionNumber(String resolutionNumber) {
    this.resolutionNumber = resolutionNumber;
  }

  public LocalDate getResolutionDate() {
    return resolutionDate;
  }

  public void setResolutionDate(LocalDate resolutionDate) {
    this.resolutionDate = resolutionDate;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public Long getRangeFrom() {
    return rangeFrom;
  }

  public void setRangeFrom(Long rangeFrom) {
    this.rangeFrom = rangeFrom;
  }

  public Long getRangeTo() {
    return rangeTo;
  }

  public void setRangeTo(Long rangeTo) {
    this.rangeTo = rangeTo;
  }

  public LocalDate getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(LocalDate validFrom) {
    this.validFrom = validFrom;
  }

  public LocalDate getValidTo() {
    return validTo;
  }

  public void setValidTo(LocalDate validTo) {
    this.validTo = validTo;
  }

  public String getTechnicalKey() {
    return technicalKey;
  }

  public void setTechnicalKey(String technicalKey) {
    this.technicalKey = technicalKey;
  }

  public Long getCurrentNumber() {
    return currentNumber;
  }

  public void setCurrentNumber(Long currentNumber) {
    this.currentNumber = currentNumber;
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
