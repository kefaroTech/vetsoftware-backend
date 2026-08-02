package com.vetsoftware.app.numberingresolution.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NumberingResolution {
  private Long id;
  private CompanyRef company;
  // Multi-sucursal (B-6): sede (prefijo) a la que aplica la resolución. null = resolución de
  // EMPRESA (prefijo
  // que sirve a todas las sedes). En la asignación, la de sede tiene prioridad sobre la de empresa
  // (fallback).
  private Long branchId;
  private ElectronicDocumentType documentType;
  private String resolutionNumber;
  private LocalDate resolutionDate;
  private String prefix;
  private Long rangeFrom;
  private Long rangeTo;
  private LocalDate validFrom;
  private LocalDate validTo;
  private String technicalKey;
  // Próximo consecutivo a emitir dentro del rango. Se gestiona al emitir documentos,
  // no por el CRUD: el create lo inicializa en rangeFrom y el update lo conserva.
  private Long currentNumber;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public NumberingResolution(
      Long id,
      CompanyRef company,
      ElectronicDocumentType documentType,
      String resolutionNumber,
      LocalDate resolutionDate,
      String prefix,
      Long rangeFrom,
      Long rangeTo,
      LocalDate validFrom,
      LocalDate validTo,
      String technicalKey,
      Long currentNumber,
      LocalDateTime createdDate,
      boolean enabled,
      Long branchId) {
    validate(
        company,
        documentType,
        resolutionNumber,
        resolutionDate,
        prefix,
        rangeFrom,
        rangeTo,
        validFrom,
        validTo,
        technicalKey,
        currentNumber);
    this.id = id;
    this.company = company;
    this.branchId = branchId;
    this.documentType = documentType;
    this.resolutionNumber = resolutionNumber;
    this.resolutionDate = resolutionDate;
    this.prefix = prefix;
    this.rangeFrom = rangeFrom;
    this.rangeTo = rangeTo;
    this.validFrom = validFrom;
    this.validTo = validTo;
    this.technicalKey = technicalKey;
    this.currentNumber = currentNumber;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static NumberingResolution create(
      CompanyRef company,
      ElectronicDocumentType documentType,
      String resolutionNumber,
      LocalDate resolutionDate,
      String prefix,
      Long rangeFrom,
      Long rangeTo,
      LocalDate validFrom,
      LocalDate validTo,
      String technicalKey,
      Long branchId) {
    return new NumberingResolution(
        null,
        company,
        documentType,
        resolutionNumber,
        resolutionDate,
        prefix,
        rangeFrom,
        rangeTo,
        validFrom,
        validTo,
        technicalKey,
        rangeFrom,
        LocalDateTime.now(),
        true,
        branchId);
  }

  public void update(
      CompanyRef company,
      ElectronicDocumentType documentType,
      String resolutionNumber,
      LocalDate resolutionDate,
      String prefix,
      Long rangeFrom,
      Long rangeTo,
      LocalDate validFrom,
      LocalDate validTo,
      String technicalKey,
      Long branchId) {
    validate(
        company,
        documentType,
        resolutionNumber,
        resolutionDate,
        prefix,
        rangeFrom,
        rangeTo,
        validFrom,
        validTo,
        technicalKey,
        this.currentNumber);
    this.company = company;
    this.branchId = branchId;
    this.documentType = documentType;
    this.resolutionNumber = resolutionNumber;
    this.resolutionDate = resolutionDate;
    this.prefix = prefix;
    this.rangeFrom = rangeFrom;
    this.rangeTo = rangeTo;
    this.validFrom = validFrom;
    this.validTo = validTo;
    this.technicalKey = technicalKey;
  }

  private static void validate(
      CompanyRef company,
      ElectronicDocumentType documentType,
      String resolutionNumber,
      LocalDate resolutionDate,
      String prefix,
      Long rangeFrom,
      Long rangeTo,
      LocalDate validFrom,
      LocalDate validTo,
      String technicalKey,
      Long currentNumber) {
    if (company == null) throw new IllegalArgumentException("company is required");
    if (documentType == null) throw new IllegalArgumentException("documentType is required");
    if (resolutionNumber == null || resolutionNumber.isBlank())
      throw new IllegalArgumentException("resolutionNumber is required");
    if (resolutionNumber.length() > 50)
      throw new IllegalArgumentException("resolutionNumber must be 50 chars or less");
    if (resolutionDate == null) throw new IllegalArgumentException("resolutionDate is required");
    if (prefix != null && prefix.length() > 10)
      throw new IllegalArgumentException("prefix must be 10 chars or less");
    if (rangeFrom == null) throw new IllegalArgumentException("rangeFrom is required");
    if (rangeFrom < 1) throw new IllegalArgumentException("rangeFrom must be 1 or greater");
    if (rangeTo == null) throw new IllegalArgumentException("rangeTo is required");
    if (rangeTo < rangeFrom)
      throw new IllegalArgumentException("rangeTo must be greater than or equal to rangeFrom");
    if (validFrom == null) throw new IllegalArgumentException("validFrom is required");
    if (validTo == null) throw new IllegalArgumentException("validTo is required");
    if (validTo.isBefore(validFrom))
      throw new IllegalArgumentException("validTo cannot be before validFrom");
    if (technicalKey != null && technicalKey.length() > 255)
      throw new IllegalArgumentException("technicalKey must be 255 chars or less");
    if (currentNumber != null && (currentNumber < rangeFrom || currentNumber > rangeTo))
      throw new IllegalArgumentException("currentNumber must be within the resolution range");
  }

  public Long getId() {
    return id;
  }

  public CompanyRef getCompany() {
    return company;
  }

  /** Sede (prefijo) de la resolución; null = resolución de empresa (todas las sedes). */
  public Long getBranchId() {
    return branchId;
  }

  public ElectronicDocumentType getDocumentType() {
    return documentType;
  }

  public String getResolutionNumber() {
    return resolutionNumber;
  }

  public LocalDate getResolutionDate() {
    return resolutionDate;
  }

  public String getPrefix() {
    return prefix;
  }

  public Long getRangeFrom() {
    return rangeFrom;
  }

  public Long getRangeTo() {
    return rangeTo;
  }

  public LocalDate getValidFrom() {
    return validFrom;
  }

  public LocalDate getValidTo() {
    return validTo;
  }

  public String getTechnicalKey() {
    return technicalKey;
  }

  public Long getCurrentNumber() {
    return currentNumber;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
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
