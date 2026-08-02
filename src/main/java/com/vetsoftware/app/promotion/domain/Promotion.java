package com.vetsoftware.app.promotion.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Promotion {
  private Long id;
  private String name;
  private PromotionType promotionType;
  private ApplicationType applicationType;
  private Long applicationItem;
  private ValueType valueType;
  private BigDecimal value;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private PromotionStatus promotionStatus;
  private CompanyRef company;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public Promotion(
      Long id,
      String name,
      PromotionType promotionType,
      ApplicationType applicationType,
      Long applicationItem,
      ValueType valueType,
      BigDecimal value,
      LocalDateTime startDate,
      LocalDateTime endDate,
      PromotionStatus promotionStatus,
      CompanyRef company,
      LocalDateTime createdDate,
      boolean enabled) {
    validate(
        name,
        promotionType,
        applicationType,
        applicationItem,
        valueType,
        value,
        startDate,
        endDate,
        promotionStatus,
        company);
    this.id = id;
    this.name = name;
    this.promotionType = promotionType;
    this.applicationType = applicationType;
    this.applicationItem = applicationItem;
    this.valueType = valueType;
    this.value = value;
    this.startDate = startDate;
    this.endDate = endDate;
    this.promotionStatus = promotionStatus;
    this.company = company;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Promotion create(
      String name,
      PromotionType promotionType,
      ApplicationType applicationType,
      Long applicationItem,
      ValueType valueType,
      BigDecimal value,
      LocalDateTime startDate,
      LocalDateTime endDate,
      PromotionStatus promotionStatus,
      CompanyRef company) {
    return new Promotion(
        null,
        name,
        promotionType,
        applicationType,
        applicationItem,
        valueType,
        value,
        startDate,
        endDate,
        promotionStatus,
        company,
        LocalDateTime.now(),
        true);
  }

  public void update(
      String name,
      PromotionType promotionType,
      ApplicationType applicationType,
      Long applicationItem,
      ValueType valueType,
      BigDecimal value,
      LocalDateTime startDate,
      LocalDateTime endDate,
      PromotionStatus promotionStatus,
      CompanyRef company) {
    validate(
        name,
        promotionType,
        applicationType,
        applicationItem,
        valueType,
        value,
        startDate,
        endDate,
        promotionStatus,
        company);
    this.name = name;
    this.promotionType = promotionType;
    this.applicationType = applicationType;
    this.applicationItem = applicationItem;
    this.valueType = valueType;
    this.value = value;
    this.startDate = startDate;
    this.endDate = endDate;
    this.promotionStatus = promotionStatus;
    this.company = company;
  }

  private static void validate(
      String name,
      PromotionType promotionType,
      ApplicationType applicationType,
      Long applicationItem,
      ValueType valueType,
      BigDecimal value,
      LocalDateTime startDate,
      LocalDateTime endDate,
      PromotionStatus promotionStatus,
      CompanyRef company) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (promotionType == null) throw new IllegalArgumentException("promotionType is required");
    if (applicationType == null) throw new IllegalArgumentException("applicationType is required");
    if (applicationItem == null || applicationItem <= 0)
      throw new IllegalArgumentException("applicationItem is required");
    if (valueType == null) throw new IllegalArgumentException("valueType is required");
    if (value == null) throw new IllegalArgumentException("value is required");
    if (value.compareTo(BigDecimal.ZERO) < 0)
      throw new IllegalArgumentException("value cannot be negative");
    if (valueType == ValueType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0)
      throw new IllegalArgumentException("percentage value cannot be greater than 100");
    if (startDate == null) throw new IllegalArgumentException("startDate is required");
    if (endDate == null) throw new IllegalArgumentException("endDate is required");
    if (endDate.isBefore(startDate))
      throw new IllegalArgumentException("endDate cannot be before startDate");
    if (promotionStatus == null) throw new IllegalArgumentException("promotionStatus is required");
    if (company == null) throw new IllegalArgumentException("company is required");
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public PromotionType getPromotionType() {
    return promotionType;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }

  public Long getApplicationItem() {
    return applicationItem;
  }

  public ValueType getValueType() {
    return valueType;
  }

  public BigDecimal getValue() {
    return value;
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  public PromotionStatus getPromotionStatus() {
    return promotionStatus;
  }

  public CompanyRef getCompany() {
    return company;
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
